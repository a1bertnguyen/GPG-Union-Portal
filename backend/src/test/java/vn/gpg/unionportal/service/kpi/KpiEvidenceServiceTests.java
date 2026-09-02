package vn.gpg.unionportal.service.kpi;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import vn.gpg.unionportal.model.Member;
import vn.gpg.unionportal.model.MemberDocument;
import vn.gpg.unionportal.model.UnionUnit;
import vn.gpg.unionportal.model.WelfareRecord;
import vn.gpg.unionportal.repository.*;
import vn.gpg.unionportal.repository.kpi.KpiNoOccurrenceConfirmationRepository;
import vn.gpg.unionportal.service.CurrentUserService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class KpiEvidenceServiceTests {
    @Test
    void resolvesMemberToWhitelistedFieldsAndExistingDownloadEndpoint() {
        Dependencies dependencies = new Dependencies();
        UnionUnit unit = unit();
        Member member = new Member();
        member.setId(10L);
        member.setUnionUnit(unit);
        member.setEmployeeCode("NV-10");
        member.setFullName("Nguyễn An");
        member.setWorkplace("Depot A");
        member.setStartWorkDate(LocalDate.of(2025, 1, 1));

        MemberDocument document = new MemberDocument();
        document.setId(91L);
        document.setMember(member);
        document.setFileName("quyet-dinh.pdf");

        when(dependencies.members.findById(10L)).thenReturn(Optional.of(member));
        when(dependencies.memberDocuments.findByMemberIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(document));

        var result = dependencies.service().read("member", "10");

        assertThat(result.sourceModule()).isEqualTo("DOAN_VIEN");
        assertThat(result.title()).contains("NV-10", "Nguyễn An");
        assertThat(result.fields()).extracting(field -> field.label())
                .contains("CĐCS", "Nơi làm việc", "Ngày vào làm");
        assertThat(result.fields()).noneMatch(field -> field.label().contains("CCCD") || field.label().contains("Điện thoại"));
        assertThat(result.attachments()).singleElement().satisfies(file -> {
            assertThat(file.fileName()).isEqualTo("quyet-dinh.pdf");
            assertThat(file.downloadPath()).isEqualTo("/member-documents/91/download");
        });
        verify(dependencies.currentUser).requireUnitAccess(unit.getId());
    }

    @Test
    void deniesSensitiveEvidenceToOrdinaryUserBeforeReturningItsFields() {
        Dependencies dependencies = new Dependencies();
        when(dependencies.currentUser.isAdmin()).thenReturn(false);

        assertThatThrownBy(() -> dependencies.service().read("welfare", "22"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("chứng cứ nhạy cảm");
        verifyNoInteractions(dependencies.welfare);
    }

    @Test
    void exposesAReportObligationEvenWhenTheSourceReportIsMissing() {
        Dependencies dependencies = new Dependencies();
        UnionUnit unit = unit();
        when(dependencies.units.findById(unit.getId())).thenReturn(Optional.of(unit));
        when(dependencies.reports.findByUnionUnitIdAndReportMonth(unit.getId(), LocalDate.of(2026, 8, 1)))
                .thenReturn(Optional.empty());

        var result = dependencies.service().read("report-obligation", "1:2026-08");

        assertThat(result.sourceRecordId()).isEqualTo("1:2026-08");
        assertThat(result.fields()).filteredOn(field -> field.label().equals("Tình trạng"))
                .singleElement().satisfies(field -> assertThat(field.value()).isEqualTo("Chưa có báo cáo"));
        verify(dependencies.currentUser).requireUnitAccess(unit.getId());
    }

    private static UnionUnit unit() {
        UnionUnit unit = new UnionUnit();
        unit.setId(1L);
        unit.setCode("REAL");
        unit.setName("CĐCS thật");
        unit.setCompanyName("GPG");
        return unit;
    }

    private static final class Dependencies {
        private final UnionUnitRepository units = mock(UnionUnitRepository.class);
        private final MemberRepository members = mock(MemberRepository.class);
        private final MemberChangeRepository memberChanges = mock(MemberChangeRepository.class);
        private final MonthlyReportRepository reports = mock(MonthlyReportRepository.class);
        private final WelfareRecordRepository welfare = mock(WelfareRecordRepository.class);
        private final LaborCaseRepository cases = mock(LaborCaseRepository.class);
        private final UnionActivityRepository activities = mock(UnionActivityRepository.class);
        private final FinanceEntryRepository finance = mock(FinanceEntryRepository.class);
        private final MemberDocumentRepository memberDocuments = mock(MemberDocumentRepository.class);
        private final WelfareDocumentRepository welfareDocuments = mock(WelfareDocumentRepository.class);
        private final LaborCaseDocumentRepository caseDocuments = mock(LaborCaseDocumentRepository.class);
        private final ActivityMediaRepository activityMedia = mock(ActivityMediaRepository.class);
        private final FinanceDocumentRepository financeDocuments = mock(FinanceDocumentRepository.class);
        private final KpiNoOccurrenceConfirmationRepository noOccurrence = mock(KpiNoOccurrenceConfirmationRepository.class);
        private final CurrentUserService currentUser = mock(CurrentUserService.class);

        private KpiEvidenceService service() {
            return new KpiEvidenceService(units, members, memberChanges, reports, welfare, cases, activities,
                    finance, memberDocuments, welfareDocuments, caseDocuments, activityMedia,
                    financeDocuments, noOccurrence, currentUser);
        }
    }
}
