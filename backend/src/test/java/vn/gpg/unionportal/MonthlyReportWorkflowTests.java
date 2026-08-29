package vn.gpg.unionportal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.controller.ReportController;
import vn.gpg.unionportal.dto.ApiModels.MonthlyReportRequest;
import vn.gpg.unionportal.model.DomainEnums.ReportStatus;
import vn.gpg.unionportal.model.MemberChange;
import vn.gpg.unionportal.model.MonthlyReport;
import vn.gpg.unionportal.repository.MemberChangeRepository;
import vn.gpg.unionportal.repository.MemberRepository;
import vn.gpg.unionportal.service.CurrentUserService;
import vn.gpg.unionportal.service.MonthlyReportService;
import vn.gpg.unionportal.service.ReportingService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class MonthlyReportWorkflowTests {
    @Autowired
    private MonthlyReportService monthlyReportService;
    @Autowired
    private ReportingService reportingService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private MemberChangeRepository memberChangeRepository;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userSubmitsOwnReportAndOnlyAdminCanApproveIt() {
        long unitId = 1L;
        authenticateUser(unitId);
        var draftRequest = request(unitId, "2031-04", ReportStatus.DRAFT);
        var draft = monthlyReportService.upsert(draftRequest);

        assertThat(draft.getStatus()).isEqualTo(ReportStatus.DRAFT);
        var submitted = monthlyReportService.upsert(request(unitId, "2031-04", ReportStatus.SUBMITTED));
        assertThat(submitted.getStatus()).isEqualTo(ReportStatus.SUBMITTED);
        assertThat(submitted.getSubmittedAt()).isNotNull();

        assertThatThrownBy(() -> monthlyReportService.update(submitted.getId(), draftRequest))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> monthlyReportService.delete(submitted.getId()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> monthlyReportService.approve(submitted.getId()))
                .isInstanceOf(AccessDeniedException.class);

        authenticateAdmin();
        var approved = monthlyReportService.approve(submitted.getId());
        assertThat(approved.getStatus()).isEqualTo(ReportStatus.APPROVED);
        assertThatThrownBy(() -> monthlyReportService.upsert(draftRequest))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void monthlySummaryCountsMemberChangesWithinTheSelectedMonthAndUnit() {
        var member = memberRepository.findAll().stream()
                .filter(item -> item.getUnionUnit().getId().equals(1L))
                .findFirst()
                .orElseThrow();
        var month = YearMonth.of(2032, 6);
        long before = reportingService.monthlySummary(month, 1L).memberChanges();
        var change = new MemberChange();
        change.setMember(member);
        change.setChangeType("Điều chuyển kiểm thử");
        change.setEffectiveDate(LocalDate.of(2032, 6, 15));
        change.setDescription("Biến động được đưa vào báo cáo tháng");
        change.setRecordedBy("test");
        memberChangeRepository.save(change);

        assertThat(reportingService.monthlySummary(month, 1L).memberChanges()).isEqualTo(before + 1);
        assertThat(reportingService.monthlySummary(YearMonth.of(2032, 7), 1L).memberChanges()).isZero();
    }

    @Test
    void approveEndpointIsBoundToPostInsteadOfReturning404() throws Exception {
        var service = mock(MonthlyReportService.class);
        var report = new MonthlyReport();
        report.setId(73L);
        when(service.approve(73L)).thenReturn(report);
        var controller = new ReportController(service, mock(ReportingService.class), mock(CurrentUserService.class));
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(post("/api/reports/73/approve"))
                .andExpect(status().isOk());
        verify(service).approve(73L);
    }

    private MonthlyReportRequest request(long unitId, String month, ReportStatus status) {
        return new MonthlyReportRequest(unitId, month, "USER CĐCS", "Kế hoạch tháng tới", "Đề xuất hỗ trợ", status);
    }

    private void authenticateUser(long unitId) {
        var now = Instant.now();
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user.vcs")
                .claim("unitId", unitId)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")), "user.vcs"));
    }

    private void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}
