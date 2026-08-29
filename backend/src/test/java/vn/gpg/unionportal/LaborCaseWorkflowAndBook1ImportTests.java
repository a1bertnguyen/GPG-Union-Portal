package vn.gpg.unionportal;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import vn.gpg.unionportal.dto.ApiModels.LaborCaseRequest;
import vn.gpg.unionportal.dto.ApiModels.CaseApprovalRequest;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.DomainEnums.CaseSeverity;
import vn.gpg.unionportal.model.DomainEnums.CaseStatus;
import vn.gpg.unionportal.repository.LaborCaseRepository;
import vn.gpg.unionportal.repository.UnionUnitRepository;
import vn.gpg.unionportal.service.LaborCaseService;
import vn.gpg.unionportal.service.SpreadsheetImportService;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LaborCaseWorkflowAndBook1ImportTests {
    @Autowired private UnionUnitRepository units;
    @Autowired private LaborCaseRepository cases;
    @Autowired private LaborCaseService service;
    @Autowired private SpreadsheetImportService spreadsheets;
    @Autowired private MockMvc mockMvc;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userProcessesCaseAndAdminIsTheOnlyFinalApprover() {
        var unit = units.findByCodeIgnoreCase("VCS").orElseThrow();
        authenticateUser(unit.getId());

        var created = service.create(request("FLOW", unit.getId(), CaseStatus.CLOSED, null));
        assertThat(created.getStatus()).isEqualTo(CaseStatus.NEW);
        assertThat(created.getOwnerName()).isNull();
        assertThat(created.getDeadline()).isNull();

        authenticateAdmin();
        var assigned = service.approve(created.getId(),
                new CaseApprovalRequest("PIC do ADMIN giao", LocalDate.now().plusDays(7)));
        assertThat(assigned.getStatus()).isEqualTo(CaseStatus.IN_PROGRESS);
        assertThat(assigned.getOwnerName()).isEqualTo("PIC do ADMIN giao");

        authenticateUser(unit.getId());
        var processed = service.update(created.getId(), request(
                created.getCaseCode(), unit.getId(), CaseStatus.IN_PROGRESS, "Đã hoàn tất phản hồi cho NLĐ"));
        assertThat(processed.getStatus()).isEqualTo(CaseStatus.IN_PROGRESS);

        var submitted = service.submitForApproval(created.getId());
        assertThat(submitted.getStatus()).isEqualTo(CaseStatus.PENDING_APPROVAL);
        assertThat(submitted.getResponseDate()).isEqualTo(LocalDate.now());
        assertThatThrownBy(() -> service.update(created.getId(), request(
                created.getCaseCode(), unit.getId(), CaseStatus.IN_PROGRESS, "Sửa sau khi gửi")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.approve(created.getId())).isInstanceOf(AccessDeniedException.class);

        authenticateAdmin();
        var approved = service.approve(created.getId());
        assertThat(approved.getStatus()).isEqualTo(CaseStatus.CLOSED);
        assertThat(approved.getApprovedBy()).isEqualTo("admin");
        assertThat(approved.getApprovedAt()).isNotNull();
    }

    @Test
    void userCanImportTheAttachedBook1LayoutIntoTheirOwnUnit() throws Exception {
        var unit = units.findByCodeIgnoreCase("VCS").orElseThrow();
        authenticateUser(unit.getId());

        var result = spreadsheets.importWorkbook("cases", book1Workbook());

        assertThat(result.errors()).isEmpty();
        assertThat(result.createdRows()).isEqualTo(1);
        var imported = cases.findAll().stream()
                .filter(item -> "NV4251".equals(item.getEmployeeCode()))
                .findFirst().orElseThrow();
        assertThat(imported.getUnionUnit().getId()).isEqualTo(unit.getId());
        assertThat(imported.getRequesterName()).isEqualTo("DƯ HỮU AN");
        assertThat(imported.getJobTitle()).isEqualTo("Lái xe nâng");
        assertThat(imported.getWorkplace()).isEqualTo("CÔNG TY CỔ PHẦN DỊCH VỤ KỸ THUẬT AZ");
        assertThat(imported.getStartWorkDate()).isEqualTo(LocalDate.of(2026, 4, 26));
        assertThat(imported.getLeaveDate()).isEqualTo(LocalDate.of(2026, 5, 13));
        assertThat(imported.getReceivedDate()).isEqualTo(LocalDate.of(2026, 7, 18));
        assertThat(imported.getResponseDate()).isEqualTo(LocalDate.of(2026, 7, 22));
        assertThat(imported.getDeadline()).isNull();
        assertThat(imported.getOwnerName()).isNull();
        assertThat(imported.getStatus()).isEqualTo(CaseStatus.NEW);
    }

    @Test
    void adminApproveRouteExistsAndClosesThePendingCase() throws Exception {
        var unit = units.findByCodeIgnoreCase("VCS").orElseThrow();
        authenticateUser(unit.getId());
        var created = service.create(request("ROUTE", unit.getId(), CaseStatus.IN_PROGRESS, "Đã xử lý xong"));
        authenticateAdmin();
        service.approve(created.getId(), new CaseApprovalRequest("PIC ADMIN", LocalDate.now().plusDays(7)));
        authenticateUser(unit.getId());
        service.update(created.getId(), request(created.getCaseCode(), unit.getId(), CaseStatus.IN_PROGRESS, "Đã xử lý xong"));
        service.submitForApproval(created.getId());
        authenticateAdmin();

        mockMvc.perform(post("/api/cases/{id}/approve", created.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.approvedBy").value("admin"));
    }

    @Test
    void overdueCaseRequiresReasonOrNewEtaBeforeItCanBeSaved() {
        var unit = units.findByCodeIgnoreCase("VCS").orElseThrow();
        authenticateUser(unit.getId());
        var created = service.create(request("OVERDUE-MISSING", unit.getId(), CaseStatus.NEW, null));
        authenticateAdmin();
        service.approve(created.getId(), new CaseApprovalRequest("PIC ADMIN", LocalDate.now()));

        var missingReason = request(created.getCaseCode(), unit.getId(), CaseStatus.IN_PROGRESS, null,
                LocalDate.now().minusDays(1), null);
        assertThatThrownBy(() -> service.update(created.getId(), missingReason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lý do quá hạn / ETA mới");

        var documented = service.update(created.getId(), request(created.getCaseCode(), unit.getId(),
                CaseStatus.IN_PROGRESS, null, LocalDate.now().minusDays(1),
                "Đang chờ đối tác; ETA mới 05/09/2026"));
        assertThat(documented.getOverdueReason()).contains("ETA mới");
    }

    @Test
    void userCaseListsAndAlertsStayInsideTheirOwnUnionUnit() {
        var ownUnit = units.findByCodeIgnoreCase("VCS").orElseThrow();
        var otherUnit = units.findByCodeIgnoreCase("GPL").orElseThrow();
        authenticateUser(ownUnit.getId());

        service.create(request("SCOPE-" + UUID.randomUUID().toString().substring(0, 8),
                ownUnit.getId(), CaseStatus.NEW, null));
        var askingForOtherUnit = new ListQuery(null, null, true, null, null,
                otherUnit.getId(), null, null);
        var visibleCases = service.search(askingForOtherUnit);
        var facets = service.facets(askingForOtherUnit);

        assertThat(visibleCases).isNotEmpty()
                .allMatch(item -> item.getUnionUnit().getId().equals(ownUnit.getId()));
        assertThat(facets.total()).isEqualTo(service.search(ListQuery.allForUnit(null)).size());
        assertThat(facets.metrics()).containsKeys("due24", "overdue", "repeated", "wideImpact", "urgentEscalation");
    }

    private LaborCaseRequest request(String code, Long unitId, CaseStatus status, String result) {
        return request(code, unitId, status, result, LocalDate.now().plusDays(7), null);
    }

    private LaborCaseRequest request(String code, Long unitId, CaseStatus status, String result,
                                     LocalDate deadline, String overdueReason) {
        return new LaborCaseRequest(
                code.startsWith("FLOW") ? "CASE-" + code + "-" + UUID.randomUUID().toString().substring(0, 8) : code,
                LocalDate.now(), unitId, "Người lao động", "NV-01", "Nhân viên",
                "VCS", LocalDate.of(2020, 1, 1), null, "0901000000", "Ứng dụng",
                "Quan hệ lao động", CaseSeverity.MEDIUM, "USER VCS", deadline,
                status, "Yêu cầu xử lý hồ sơ", 1,
                result == null ? null : "https://example.test/ket-qua.pdf", result, null, overdueReason);
    }

    private MockMultipartFile book1Workbook() throws Exception {
        String[] headers = {"MãNV", "Họ Và Tên", "Chức danh", "Noi làm việc", "Ngày vào làm",
                "ngày nghỉ", "Điện thoại", "Yêu cầu", "Ngày yêu cầu", "Ngày trả lời"};
        String[] values = {"NV4251", "DƯ HỮU AN", "Lái xe nâng", "CÔNG TY CỔ PHẦN DỊCH VỤ KỸ THUẬT AZ",
                "26/4/2026", "13/5/2026", "0901234567", "Chốt sổ BHXH và quyết định nghỉ việc",
                "18/7/2026", "22/7/2026"};
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Sheet2");
            var header = sheet.createRow(0);
            var row = sheet.createRow(1);
            for (int index = 0; index < headers.length; index++) {
                header.createCell(index).setCellValue(headers[index]);
                row.createCell(index).setCellValue(values[index]);
            }
            workbook.write(output);
            return new MockMultipartFile("file", "Book1.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }

    private void authenticateUser(Long unitId) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("case-user-token")
                .header("alg", "none")
                .subject("user.vcs")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("unitId", unitId)
                .claim("roles", List.of("USER"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")), "user.vcs"));
    }

    private void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}
