package vn.gpg.unionportal.service.kpi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.KpiModels.PeriodType;
import vn.gpg.unionportal.dto.KpiModels.VersionWindow;
import vn.gpg.unionportal.model.kpi.KpiDefinition;
import vn.gpg.unionportal.repository.kpi.KpiDefinitionRepository;
import vn.gpg.unionportal.repository.kpi.KpiVersionRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guards the KPI version window and catalog contract against the whole live migration chain.
 *
 * <p>V21, V22 and V23 each seeded a catalog with {@code effective_from = 2026-01-01} and no
 * {@code effective_to}, so three windows covered every period identically and only the {@code status}
 * column stopped {@link GpgKpiEngine} from throwing "nhiều phiên bản KPI cùng bao phủ kỳ". V25 closes the
 * two superseded windows; these tests fail if that regresses, and if the catalog validator ever goes back
 * to reporting a code count that does not match the version it actually checked.
 */
@SpringBootTest
@Transactional
class KpiVersionCatalogTests {
    private static final String V1 = "GPG-CD-KPI-V1";
    private static final String V2 = "GPG-CD-KPI-V2";
    private static final String V3 = "GPG-CD-KPI-V3";

    @Autowired private GpgKpiEngine engine;
    @Autowired private KpiVersionRepository versions;
    @Autowired private KpiDefinitionRepository definitions;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exactlyOneVersionIsSelectableAfterTheFullMigrationChain() {
        assertThat(engine.metadata().versions())
                .extracting(VersionWindow::versionId)
                .containsExactly(V3);
    }

    @Test
    void v25ClosesTheWindowsOfBothSupersededVersionsAndLeavesTheActiveOneOpen() {
        for (String versionId : List.of(V1, V2)) {
            var version = versions.findById(versionId).orElseThrow();
            assertThat(version.getStatus()).as("%s status", versionId).isEqualTo("SUPERSEDED");
            assertThat(version.getEffectiveTo()).as("%s effective_to", versionId)
                    .isEqualTo(version.getEffectiveFrom());
        }
        assertThat(versions.findById(V3).orElseThrow().getEffectiveTo()).isNull();
    }

    /**
     * The landmine V25 defuses: someone flipping a superseded catalog back to ACTIVE — to look at an old
     * period, say — used to make GET /api/kpi answer 500 for every unit.
     */
    @Test
    void reactivatingASupersededVersionNoLongerBreaksTheDashboard() {
        var superseded = versions.findById(V2).orElseThrow();
        superseded.setStatus("ACTIVE");
        versions.saveAndFlush(superseded);
        authenticateAdmin();

        var dashboard = engine.evaluate(PeriodType.MONTH, 2026, 8, null);

        assertThat(dashboard.versionId()).isEqualTo(V3);
        // metadata() lists every catalog that is selectable by status, so the reactivated one shows up again --
        // but it must carry the closed window, which is how a client can tell it covers no current period.
        assertThat(engine.metadata().versions())
                .filteredOn(window -> V2.equals(window.versionId()))
                .singleElement()
                .satisfies(window -> assertThat(window.effectiveTo()).isEqualTo(window.effectiveFrom()));
    }

    @Test
    void theActiveCatalogIsTwentyFourCodesWorthOneHundredPointsAndIncludesCare05() {
        var catalog = definitions.findByVersionIdOrderById(V3);

        assertThat(catalog).hasSize(24);
        assertThat(catalog).extracting(KpiDefinition::getKpiCode).contains("CARE05");
        assertThat(catalog.stream().map(KpiDefinition::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("100");
        assertThat(catalog).extracting(KpiDefinition::getGroupCode).doesNotContainNull();
    }

    /**
     * The old message hardcoded {@code EXPECTED_CODES.size()} — it said "phải có đúng 23 mã" while the V3
     * catalog it had just rejected needs 24, sending whoever debugged it down the wrong path.
     */
    @Test
    void aCatalogMissingACodeReportsTheVersionTheRightCountAndTheMissingCode() {
        var care05 = definitionOf(V3, "CARE05");
        definitions.delete(care05);
        definitions.flush();
        authenticateAdmin();

        assertThatThrownBy(() -> engine.evaluate(PeriodType.MONTH, 2026, 8, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(V3)
                .hasMessageContaining("phải có đúng 24 mã, đang có 23")
                .hasMessageContaining("thiếu mã [CARE05]")
                .hasMessageContaining("tổng trọng số phải bằng 100");
    }

    /** A code with no calculator only ever yields MISSING_DATA, so the catalog must refuse to load it. */
    @Test
    void aCodeWithoutACalculatorIsNamedInTheError() {
        var renamed = definitionOf(V3, "CARE05");
        renamed.setKpiCode("CARE99");
        definitions.saveAndFlush(renamed);
        authenticateAdmin();

        assertThatThrownBy(() -> engine.evaluate(PeriodType.MONTH, 2026, 8, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(V3)
                .hasMessageContaining("thiếu mã [CARE05]")
                .hasMessageContaining("mã chưa có calculator [CARE99]")
                .hasMessageNotContaining("phải có đúng");
    }

    /** Group weights must stay complete: dropping a whole group has to be reported as such, not as a count. */
    @Test
    void aCatalogWhoseWeightsNoLongerSumToOneHundredIsRejected() {
        var definition = definitionOf(V3, "GOV01");
        definition.setWeight(definition.getWeight().add(BigDecimal.ONE));
        definitions.saveAndFlush(definition);
        authenticateAdmin();

        assertThatThrownBy(() -> engine.evaluate(PeriodType.MONTH, 2026, 8, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tổng trọng số phải bằng 100, đang là 101");
    }

    private KpiDefinition definitionOf(String versionId, String kpiCode) {
        return definitions.findByVersionIdOrderById(versionId).stream()
                .filter(item -> kpiCode.equals(item.getKpiCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Không tìm thấy " + kpiCode + " trong " + versionId));
    }

    private void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}
