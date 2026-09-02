package vn.gpg.unionportal;

import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.model.DomainEnums.IntegrationStatus;
import vn.gpg.unionportal.model.DomainEnums.WelfarePolicySource;
import vn.gpg.unionportal.model.DomainEnums.WelfareType;
import vn.gpg.unionportal.model.WelfarePolicy;
import vn.gpg.unionportal.repository.*;
import vn.gpg.unionportal.service.SpreadsheetImportService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SpreadsheetImportServiceTests {
    private static final List<String> RESOURCES = List.of(
            "units", "members", "welfare", "cases", "activities", "finance",
            "surveys", "survey-responses", "reports", "users");

    @Autowired private SpreadsheetImportService service;
    @Autowired private UnionUnitRepository unitRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private WelfareRecordRepository welfareRepository;
    @Autowired private WelfarePolicyRepository welfarePolicyRepository;
    @Autowired private LaborCaseRepository caseRepository;
    @Autowired private UnionActivityRepository activityRepository;
    @Autowired private FinanceEntryRepository financeRepository;
    @Autowired private PulseSurveyRepository surveyRepository;
    @Autowired private PulseSurveyResponseRepository responseRepository;
    @Autowired private MonthlyReportRepository reportRepository;
    @Autowired private AdminUserRepository userRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsOpenableTemplateWithDataAndGuideSheetsForEveryResource() throws Exception {
        for (String resource : RESOURCES) {
            byte[] bytes = service.createTemplate(resource);
            assertThat(bytes).isNotEmpty();
            try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
                assertThat(workbook.getSheet("Dữ liệu")).isNotNull();
                assertThat(workbook.getSheet("Hướng dẫn")).isNotNull();
                assertThat(workbook.getSheet("Dữ liệu").getRow(0).getPhysicalNumberOfCells()).isGreaterThan(0);
                var dataSheet = workbook.getSheet("Dữ liệu");
                assertThat(dataSheet.getRow(0).getCell(0).getStringCellValue())
                        .as(resource).doesNotMatch("[a-z][A-Za-z]+Code|username|unitCode");
                for (var cell : dataSheet.getRow(0)) {
                    int readableWidth = Math.min(cell.getStringCellValue().length() + 3, 55) * 256;
                    assertThat(dataSheet.getColumnWidth(cell.getColumnIndex())).as(resource + " column " + cell.getColumnIndex())
                            .isGreaterThanOrEqualTo(readableWidth);
                }
                assertThat(workbook.getSheet("Hướng dẫn").getRow(0).getCell(1).getStringCellValue())
                        .isEqualTo("Mã kỹ thuật");
                assertThat(workbook.getSheet("Hướng dẫn").getLastRowNum()).isGreaterThan(1);
            }
        }
    }

    @Test
    void welfareTemplateOffersActivePoliciesAndExplainsTheImportRules() throws Exception {
        WelfarePolicy activePolicy = welfarePolicyRepository.findByCodeIgnoreCase("CD-01-01").orElseThrow();
        WelfarePolicy inactivePolicy = new WelfarePolicy();
        inactivePolicy.setCode("XLS-INACTIVE-POLICY");
        inactivePolicy.setSource(WelfarePolicySource.UNION);
        inactivePolicy.setSequenceNumber(9_999);
        inactivePolicy.setWelfareType(WelfareType.HARDSHIP);
        inactivePolicy.setName("Chính sách ngừng áp dụng để kiểm thử");
        inactivePolicy.setSupportAmount(new java.math.BigDecimal("100000"));
        inactivePolicy.setProcessingWeeks(1);
        inactivePolicy.setActive(false);
        welfarePolicyRepository.save(inactivePolicy);

        byte[] bytes = service.createTemplate("welfare");

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            var dataSheet = workbook.getSheet("Dữ liệu");
            var rulesSheet = workbook.getSheet("Quy tắc nhập liệu");
            var policiesSheet = workbook.getSheet("Danh mục chính sách");
            assertThat(rulesSheet).isNotNull();
            assertThat(policiesSheet).isNotNull();
            assertThat(workbook.getName("activeWelfarePolicyChoices")).isNotNull();
            assertThat(policiesSheet.getRow(3).getCell(0).getStringCellValue()).startsWith(activePolicy.getCode() + " — ");
            for (int rowIndex = 3; rowIndex <= policiesSheet.getLastRowNum(); rowIndex++) {
                assertThat(policiesSheet.getRow(rowIndex).getCell(1).getStringCellValue()).isNotEqualTo(inactivePolicy.getCode());
            }
            int policyColumn = columnIndex(dataSheet, "Chính sách chăm lo");
            assertThat(policyColumn).isGreaterThanOrEqualTo(0);
            assertThat(dataSheet.getDataValidations()).anySatisfy(validation ->
                    assertThat(validation.getValidationConstraint().getFormula1()).isEqualTo("activeWelfarePolicyChoices"));
            assertThat(rulesSheet.getRow(3).getCell(1).getStringCellValue()).isEqualTo("Chọn chính sách");
        }
    }

    @Test
    void importsTheFormerWelfareTemplateWithoutASelectedPolicy() throws Exception {
        byte[] input;
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Dữ liệu");
            String[] headers = {"Mã hồ sơ", "Loại chăm lo", "Tên chính sách / định mức áp dụng", "Mã CĐCS",
                    "Người thụ hưởng", "Ngày sự kiện", "Hạn hoàn tất", "Trạng thái xử lý", "Số tiền", "Định mức",
                    "Tình trạng hồ sơ", "Tình trạng biên nhận", "Có hình ảnh", "Ghi chú"};
            var header = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) header.createCell(index).setCellValue(headers[index]);
            var row = sheet.createRow(1);
            String[] values = {"XLS-LEGACY-WELFARE", "VISIT", "Chăm lo cũ", "VCS", "Đoàn viên cũ", "2026-03-01",
                    "2026-03-08", "COMPLETED", "250000", "200000", "COMPLETE", "COMPLETE", "TRUE", "File cũ"};
            for (int index = 0; index < values.length; index++) row.createCell(index).setCellValue(values[index]);
            workbook.write(output);
            input = output.toByteArray();
        }

        var result = service.importWorkbook("welfare", new MockMultipartFile("file", "welfare-legacy.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", input));

        assertThat(result.errors()).isEmpty();
        var imported = welfareRepository.findByRecordCodeIgnoreCase("XLS-LEGACY-WELFARE").orElseThrow();
        assertThat(imported.getPolicyId()).isNull();
        assertThat(imported.getPolicyName()).isEqualTo("Chăm lo cũ");
        assertThat(imported.getAmount()).isEqualByComparingTo("250000");
    }

    @Test
    void importsAllEditableFieldsAcrossEveryResourceAndWritesAuditRuns() throws Exception {
        assertSuccessful("units", values(
                "code", "XLS-UNIT", "name", "CĐCS Excel", "companyName", "Công ty Excel",
                "location", "Hà Nội", "chairperson", "Nguyễn Chủ tịch", "termStart", "2026-01-01",
                "termEnd", "2030-12-31", "decisionNumber", "QD-XLS", "legalStatus", "ACTIVE",
                "contactPerson", "Trần Đầu mối"));

        assertSuccessful("members", values(
                "employeeCode", "XLS-MEMBER", "fullName", "Đoàn viên Excel", "unitCode", "XLS-UNIT",
                "jobTitle", "Chuyên viên", "workplace", "VP-TCT", "joinDate", "2026-02-01",
                "membershipStatus", "MEMBER", "employmentStatus", "ACTIVE", "email", "excel@gpg.vn", "phone", "0901234567"));
        assertSuccessful("welfare", values(
                "recordCode", "XLS-WELFARE", "policyCode", "CD-01-01", "unitCode", "XLS-UNIT",
                "beneficiaryName", "Người thụ hưởng", "eventDate", "2026-03-01", "status", "COMPLETED",
                "documentStatus", "COMPLETE", "notes", "Nhập đủ trường từ Excel"));
        assertSuccessful("cases", values(
                "caseCode", "XLS-CASE", "receivedDate", "2026-03-02", "unitCode", "XLS-UNIT",
                "issueGroup", "Điều kiện làm việc", "severity", "HIGH", "ownerName", "PIC Excel",
                "deadline", "2026-03-20", "status", "IN_PROGRESS", "description", "Mô tả vụ việc",
                "affectedPeople", "4", "resultText", "Đang xử lý", "overdueReason", "Không quá hạn"));
        assertSuccessful("activities", values(
                "activityCode", "XLS-ACT", "name", "Hoạt động Excel", "unitCode", "XLS-UNIT",
                "eventDate", "2026-04-01", "status", "APPROVED", "objective", "Kết nối NLĐ",
                "plannedBudget", "3000000", "actualCost", "1500000", "participantCount", "25",
                "usefulnessScore", "4.5", "reportCompleted", "FALSE", "followUpOwner", "PIC Follow-up",
                "followUpDeadline", "2026-04-10"));
        assertSuccessful("finance", values(
                "entryCode", "XLS-FIN", "unitCode", "XLS-UNIT", "transactionDate", "2026-04-02",
                "entryType", "EXPENSE", "category", "Hoạt động", "amount", "1500000",
                "description", "Chi nội bộ từ Excel", "documentNumber", "PC-XLS",
                "documentStatus", "COMPLETE"));
        assertSuccessful("surveys", values(
                "surveyCode", "XLS-SURVEY", "title", "Khảo sát Excel", "unitCode", "XLS-UNIT",
                "questionText", "Bạn đánh giá hoạt động thế nào?", "startDate", "2026-05-01",
                "endDate", "2026-05-31", "status", "ACTIVE", "targetResponses", "20"));
        assertSuccessful("survey-responses", values(
                "surveyCode", "XLS-SURVEY", "rating", "5", "needCategory", "Phúc lợi",
                "suggestion", "Tăng chương trình chăm lo", "anonymous", "FALSE",
                "respondentName", "Người phản hồi", "submittedOn", "2026-05-10"));
        assertSuccessful("reports", values(
                "unitCode", "XLS-UNIT", "month", "2026-05", "preparedBy", "Người lập Excel",
                "planNextMonth", "Kế hoạch tháng 6", "supportRequest", "Đề xuất hỗ trợ",
                "status", "SUBMITTED"));
        assertSuccessful("users", values(
                "username", "xls.user", "fullName", "Tài khoản Excel", "role", "USER",
                "unitCode", "XLS-UNIT", "active", "TRUE", "password", "Excel@123!"));

        assertThat(unitRepository.findByCodeIgnoreCase("XLS-UNIT")).get().extracting("contactPerson").isEqualTo("Trần Đầu mối");
        assertThat(memberRepository.findByEmployeeCodeIgnoreCase("XLS-MEMBER")).get().extracting("email").isEqualTo("excel@gpg.vn");
        assertThat(welfareRepository.findByRecordCodeIgnoreCase("XLS-WELFARE")).isPresent();
        var importedWelfare = welfareRepository.findByRecordCodeIgnoreCase("XLS-WELFARE").orElseThrow();
        WelfarePolicy selectedPolicy = welfarePolicyRepository.findByCodeIgnoreCase("CD-01-01").orElseThrow();
        assertThat(importedWelfare.getPolicyId()).isEqualTo(selectedPolicy.getId());
        assertThat(importedWelfare.getWelfareType()).isEqualTo(selectedPolicy.getWelfareType());
        assertThat(importedWelfare.getAmount()).isEqualByComparingTo(selectedPolicy.getSupportAmount());
        assertThat(caseRepository.findByCaseCodeIgnoreCase("XLS-CASE")).isPresent();
        assertThat(activityRepository.findByActivityCodeIgnoreCase("XLS-ACT")).isPresent();
        assertThat(financeRepository.findByEntryCodeIgnoreCase("XLS-FIN")).isPresent();
        var survey = surveyRepository.findBySurveyCodeIgnoreCase("XLS-SURVEY").orElseThrow();
        assertThat(responseRepository.countBySurveyId(survey.getId())).isEqualTo(1);
        assertThat(reportRepository.findByUnionUnitIdAndReportMonth(unitRepository.findByCodeIgnoreCase("XLS-UNIT").orElseThrow().getId(),
                java.time.LocalDate.of(2026, 5, 1))).isPresent();
        assertThat(userRepository.findByUsernameIgnoreCase("xls.user")).isPresent();
    }

    @Test
    void updatesByBusinessKeyAndRejectsRowsOutsideUserUnitScope() throws Exception {
        var first = assertSuccessful("members", values(
                "employeeCode", "XLS-UPSERT", "fullName", "Tên cũ", "unitCode", "VCS",
                "membershipStatus", "MEMBER", "employmentStatus", "ACTIVE"));
        var second = service.importWorkbook("members", workbook("members", values(
                "employeeCode", "XLS-UPSERT", "fullName", "Tên mới", "unitCode", "VCS",
                "membershipStatus", "MEMBER", "employmentStatus", "ACTIVE")));

        assertThat(first.createdRows()).isEqualTo(1);
        assertThat(second.createdRows()).isZero();
        assertThat(second.updatedRows()).isEqualTo(1);
        assertThat(memberRepository.findByEmployeeCodeIgnoreCase("XLS-UPSERT")).get().extracting("fullName").isEqualTo("Tên mới");

        authenticateUserForUnit(1L);
        var blocked = service.importWorkbook("members", workbook("members", values(
                "employeeCode", "XLS-CROSS-SCOPE", "fullName", "Sai phạm vi", "unitCode", "GPL",
                "membershipStatus", "MEMBER", "employmentStatus", "ACTIVE")));

        assertThat(blocked.run().getStatus()).isEqualTo(IntegrationStatus.FAILED);
        assertThat(blocked.errors()).anyMatch(error -> error.contains("chỉ được truy cập dữ liệu thuộc CĐCS"));
        assertThat(memberRepository.findByEmployeeCodeIgnoreCase("XLS-CROSS-SCOPE")).isEmpty();
    }

    @Test
    void userExportsAndImportsOnlyMonthlyReportsFromTheirAssignedUnit() throws Exception {
        assertSuccessful("reports", values(
                "unitCode", "VCS", "month", "2034-01", "preparedBy", "Người lập VCS",
                "planNextMonth", "Kế hoạch VCS", "supportRequest", "Hỗ trợ VCS", "status", "DRAFT"));
        assertSuccessful("reports", values(
                "unitCode", "GPL", "month", "2034-01", "preparedBy", "Người lập GPL",
                "planNextMonth", "Kế hoạch GPL", "supportRequest", "Hỗ trợ GPL", "status", "DRAFT"));

        authenticateUserForUnit(1L);
        byte[] exported = service.exportReports("2034-01", null);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(exported))) {
            var sheet = workbook.getSheet("Dữ liệu");
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("VCS");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("2034-01");
        }

        var ownSubmission = service.importWorkbook("reports", workbook("reports", values(
                "unitCode", "VCS", "month", "2034-01", "preparedBy", "USER VCS",
                "planNextMonth", "Kế hoạch đã nhập", "supportRequest", "", "status", "SUBMITTED")));
        assertThat(ownSubmission.errors()).isEmpty();
        assertThat(ownSubmission.updatedRows()).isEqualTo(1);

        var locked = service.importWorkbook("reports", workbook("reports", values(
                "unitCode", "VCS", "month", "2034-01", "preparedBy", "USER VCS",
                "planNextMonth", "Ghi đè", "supportRequest", "", "status", "DRAFT")));
        assertThat(locked.errors()).anyMatch(error -> error.contains("đã nộp cho ADMIN"));

        var approved = service.importWorkbook("reports", workbook("reports", values(
                "unitCode", "VCS", "month", "2034-02", "preparedBy", "USER VCS",
                "planNextMonth", "Không hợp lệ", "supportRequest", "", "status", "APPROVED")));
        assertThat(approved.errors()).anyMatch(error -> error.contains("không được nhập báo cáo ở trạng thái"));

        var crossUnit = service.importWorkbook("reports", workbook("reports", values(
                "unitCode", "GPL", "month", "2034-02", "preparedBy", "USER VCS",
                "planNextMonth", "Sai phạm vi", "supportRequest", "", "status", "DRAFT")));
        assertThat(crossUnit.errors()).anyMatch(error -> error.contains("chỉ được truy cập dữ liệu thuộc CĐCS"));
    }

    @Test
    void rejectsMemberCompanyAndWorkplaceValuesOutsideTheReferenceCatalog() throws Exception {
        var badCompany = service.importWorkbook("members", workbook("members", values(
                "employeeCode", "XLS-BAD-COMPANY", "fullName", "Sai công ty", "unitCode", "VCS",
                "company", "Công ty tự nhập", "workplace", "VP-TCT",
                "membershipStatus", "MEMBER", "employmentStatus", "ACTIVE")));
        var badWorkplace = service.importWorkbook("members", workbook("members", values(
                "employeeCode", "XLS-BAD-WORKPLACE", "fullName", "Sai nơi làm việc", "unitCode", "VCS",
                "company", "CÔNG TY CỔ PHẦN DỊCH VỤ KỸ THUẬT AZ", "workplace", "Nơi tự nhập",
                "membershipStatus", "MEMBER", "employmentStatus", "ACTIVE")));

        assertThat(badCompany.errors()).anyMatch(error -> error.contains("Công ty phải được chọn từ danh mục"));
        assertThat(badWorkplace.errors()).anyMatch(error -> error.contains("Nơi làm việc phải được chọn từ danh mục"));
        assertThat(memberRepository.findByEmployeeCodeIgnoreCase("XLS-BAD-COMPANY")).isEmpty();
        assertThat(memberRepository.findByEmployeeCodeIgnoreCase("XLS-BAD-WORKPLACE")).isEmpty();
    }

    private vn.gpg.unionportal.dto.ApiModels.SpreadsheetImportResult assertSuccessful(String resource,
                                                                                             Map<String, String> values) throws Exception {
        var result = service.importWorkbook(resource, workbook(resource, values));
        assertThat(result.errors()).as(resource).isEmpty();
        assertThat(result.run().getStatus()).as(resource).isEqualTo(IntegrationStatus.COMPLETED);
        assertThat(result.run().getSuccessfulRows()).as(resource).isEqualTo(1);
        return result;
    }

    private MockMultipartFile workbook(String resource, Map<String, String> values) throws Exception {
        byte[] template = service.createTemplate(resource);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(template));
             var output = new ByteArrayOutputStream()) {
            var sheet = workbook.getSheet("Dữ liệu");
            var header = sheet.getRow(0);
            var displayedIndexes = new LinkedHashMap<String, Integer>();
            for (var cell : header) displayedIndexes.put(cell.getStringCellValue(), cell.getColumnIndex());
            var indexes = new LinkedHashMap<String, Integer>();
            var guide = workbook.getSheet("Hướng dẫn");
            for (int rowIndex = 1; rowIndex <= guide.getLastRowNum(); rowIndex++) {
                var guideRow = guide.getRow(rowIndex);
                if (guideRow == null || guideRow.getCell(1) == null) continue;
                String technicalName = guideRow.getCell(1).getStringCellValue();
                if (technicalName.isBlank()) continue;
                indexes.put(technicalName, displayedIndexes.get(guideRow.getCell(0).getStringCellValue()));
            }
            var row = sheet.createRow(1);
            values.forEach((name, value) -> row.createCell(indexes.get(name)).setCellValue(value));
            workbook.write(output);
            return new MockMultipartFile("file", resource + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }

    private Map<String, String> values(String... pairs) {
        var result = new LinkedHashMap<String, String>();
        for (int index = 0; index < pairs.length; index += 2) result.put(pairs[index], pairs[index + 1]);
        return result;
    }

    private int columnIndex(org.apache.poi.ss.usermodel.Sheet sheet, String headerText) {
        for (var cell : sheet.getRow(0)) {
            if (headerText.equals(cell.getStringCellValue())) return cell.getColumnIndex();
        }
        return -1;
    }

    private void authenticateUserForUnit(Long unitId) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("spreadsheet-test-token")
                .header("alg", "HS256")
                .subject("user.vcs")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("roles", List.of("USER"))
                .claim("unitId", unitId)
                .build();
        var authentication = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")), "user.vcs");
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
