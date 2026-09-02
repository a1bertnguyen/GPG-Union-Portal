package vn.gpg.unionportal.service.kpi;

import org.junit.jupiter.api.Test;
import vn.gpg.unionportal.dto.KpiModels.*;
import vn.gpg.unionportal.model.kpi.KpiClassificationRule;
import vn.gpg.unionportal.model.kpi.KpiDefinition;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KpiScoringPolicyTests {

    @Test
    void resolvesAllFourSupportedPeriodTypes() {
        assertThat(GpgKpiEngine.resolvePeriod(PeriodType.MONTH, 2026, 2))
                .extracting(Period::periodStart, Period::periodEnd)
                .containsExactly(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        assertThat(GpgKpiEngine.resolvePeriod(PeriodType.QUARTER, 2026, 3))
                .extracting(Period::periodStart, Period::periodEnd)
                .containsExactly(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30));
        assertThat(GpgKpiEngine.resolvePeriod(PeriodType.HALF_YEAR, 2026, 2))
                .extracting(Period::periodStart, Period::periodEnd)
                .containsExactly(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31));
        assertThat(GpgKpiEngine.resolvePeriod(PeriodType.YEAR, 2026, 1))
                .extracting(Period::periodStart, Period::periodEnd)
                .containsExactly(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertThatThrownBy(() -> GpgKpiEngine.resolvePeriod(PeriodType.QUARTER, 2026, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalizesDirectionsWithoutRoundingIntermediateValues() {
        KpiDefinition higher = definition("HIGHER_BETTER", "0.8", null, "5");
        BigDecimal normalized = KpiScoringPolicy.normalized(higher, BigDecimal.ONE, BigDecimal.valueOf(3));
        assertThat(normalized).isGreaterThan(new BigDecimal("41.66")).isLessThan(new BigDecimal("41.67"));
        assertThat(KpiScoringPolicy.earnedPoints(normalized, higher.getWeight()))
                .isGreaterThan(new BigDecimal("2.083")).isLessThan(new BigDecimal("2.084"));

        KpiDefinition lower = definition("LOWER_BETTER", "0.1", "0.5", "2");
        assertThat(KpiScoringPolicy.normalized(lower, BigDecimal.ONE, BigDecimal.valueOf(4)))
                .isEqualByComparingTo("62.5");

        KpiDefinition rating = definition("RATING_1_5", null, null, "2");
        assertThat(KpiScoringPolicy.normalized(rating, BigDecimal.valueOf(4), BigDecimal.ONE))
                .isEqualByComparingTo("75");

        KpiDefinition unsupported = definition("UNKNOWN", "1", null, "2");
        assertThat(KpiScoringPolicy.normalized(unsupported, BigDecimal.ONE, BigDecimal.ONE)).isNull();
    }

    @Test
    void missingDataKeepsWeightWhileConfirmedNaRedistributesIt() {
        Detail calculated = detail("A", new BigDecimal("5"), new BigDecimal("4"), ResultStatus.CALCULATED);
        Detail missing = detail("B", new BigDecimal("5"), BigDecimal.ZERO, ResultStatus.MISSING_DATA);
        Detail na = detail("C", BigDecimal.ZERO, BigDecimal.ZERO, ResultStatus.NA);

        assertThat(KpiScoringPolicy.baseScore(List.of(calculated, missing, na)))
                .isEqualByComparingTo("40");
        assertThat(KpiScoringPolicy.baseScore(List.of(calculated, na)))
                .isEqualByComparingTo("80");
    }

    @Test
    void classificationThresholdsComeFromVersionRules() {
        List<KpiClassificationRule> rules = List.of(rule("Xuất sắc", "90", 1), rule("Tốt", "80", 2),
                rule("Khá", "65", 3), rule("Trung bình", "50", 4), rule("Không đạt", "0", 5));
        assertThat(KpiScoringPolicy.classification(new BigDecimal("89.999"), rules)).isEqualTo("Tốt");
        assertThat(KpiScoringPolicy.classification(new BigDecimal("49.999"), rules)).isEqualTo("Không đạt");
    }

    @Test
    void catalogAndSeedContainExactlyTheSpecifiedThirtyOneCodes() throws Exception {
        assertThat(GpgKpiEngine.EXPECTED_CODES).hasSize(31);
        String migration = Files.readString(Path.of("src/main/resources/db/migration/V21__create_gpg_kpi_engine.sql"));
        for (String code : GpgKpiEngine.EXPECTED_CODES) {
            assertThat(migration).contains("'" + code + "'");
        }
    }

    @Test
    void officialRankingUsesCareThenGrievanceThenReportThenPenaltyAndPreservesTies() {
        UnitResult lowerCare = unit("B", "85", "70", "90", "1", "1");
        UnitResult higherCare = unit("A", "85", "80", "80", "1", "2");
        UnitResult sameTie = unit("C", "85", "80", "80", "1", "2");
        List<UnitResult> sorted = List.of(lowerCare, sameTie, higherCare).stream()
                .sorted(GpgKpiEngine.rankingComparator()).toList();
        List<UnitResult> ranked = GpgKpiEngine.rankOfficial(sorted);

        assertThat(ranked).extracting(UnitResult::unionUnitCode).containsExactly("A", "C", "B");
        assertThat(ranked).extracting(UnitResult::rank).containsExactly(1, 1, 3);
        assertThat(ranked.get(0).tied()).isTrue();
        assertThat(ranked.get(1).tied()).isTrue();
    }

    @Test
    void rankingKeepsOfficialTieAcrossProvisionalRowsAndIgnoresDecimalScale() {
        UnitResult first = unit("A", "85.0", "80.00", "80", "1.0", "2.00");
        UnitResult provisional = withStatus(unit("P", "85", "80", "80", "1", "2"), RunStatus.PROVISIONAL);
        UnitResult second = unit("B", "85.00", "80", "80.0", "1.00", "2");

        List<UnitResult> ranked = GpgKpiEngine.rankOfficial(List.of(first, provisional, second));

        assertThat(ranked.get(0).rank()).isEqualTo(1);
        assertThat(ranked.get(0).tied()).isTrue();
        assertThat(ranked.get(1).rank()).isNull();
        assertThat(ranked.get(2).rank()).isEqualTo(1);
        assertThat(ranked.get(2).tied()).isTrue();
    }

    private KpiDefinition definition(String direction, String target, String max, String weight) {
        KpiDefinition result = new KpiDefinition();
        result.setDirection(direction);
        result.setTargetValue(target == null ? null : new BigDecimal(target));
        result.setMaxAllowedValue(max == null ? null : new BigDecimal(max));
        result.setWeight(new BigDecimal(weight));
        return result;
    }

    private KpiClassificationRule rule(String label, String minimum, int order) {
        KpiClassificationRule result = new KpiClassificationRule();
        result.setLabel(label);
        result.setMinimumScore(new BigDecimal(minimum));
        result.setSortOrder(order);
        return result;
    }

    private Detail detail(String code, BigDecimal eligibleWeight, BigDecimal earned, ResultStatus status) {
        return new Detail(code, code, "DATA", code, eligibleWeight, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.valueOf(100), eligibleWeight, earned, status, "", List.of(), List.of(), 0);
    }

    private UnitResult unit(String code, String score, String care, String grievance, String report, String penalty) {
        List<GroupResult> groups = List.of(group("CARE", care), group("GRV", grievance));
        return new UnitResult("RUN-" + code, (long) code.charAt(0), code, code, 1L, RunStatus.FINAL,
                BigDecimal.ONE, new BigDecimal(score), BigDecimal.ZERO, new BigDecimal(penalty),
                new BigDecimal(score), "Tốt", "Tốt", null, false, new BigDecimal(report), groups,
                List.of(), List.of(), List.of());
    }

    private UnitResult withStatus(UnitResult item, RunStatus status) {
        return new UnitResult(item.runId(), item.unionUnitId(), item.unionUnitCode(), item.unionUnitName(),
                item.activeMemberCount(), status, item.dataQualityRate(), item.baseScore(), item.bonusPoints(),
                item.penaltyPoints(), item.finalScore(), item.rawClassification(), item.finalClassification(),
                item.rank(), item.tied(), item.reportOnTimeRate(), item.groups(), item.details(), item.warnings(),
                item.adjustments());
    }

    private GroupResult group(String code, String score) {
        return new GroupResult(code, code, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE,
                new BigDecimal(score), ResultStatus.CALCULATED, List.of());
    }
}
