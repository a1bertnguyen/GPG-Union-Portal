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
    void importsAllEditableFieldsAcrossEveryResourceAndWritesAuditRuns() throws Exception {
        assertSuccessful("units", values(
                "code", "XLS-UNIT", "name", "CĐCS Excel", "companyName", "Công ty Excel",
                "location", "Hà Nội", "chairperson", "Nguyễn Chủ tịch", "termStart", "2026-01-01",
                "termEnd", "2030-12-31", "decisionNumber", "QD-XLS", "legalStatus", "ACTIVE",
                "contactPerson", "Trần Đầu mối"));

        assertSuccessful("members", values(
                "employeeCode", "XLS-MEMBER", "fullName", "Đoàn viên Excel", "unitCode", "XLS-UNIT",
                "jobTitle", "Chuyên viên", "workplace", "Văn phòng", "joinDate", "2026-02-01",
                "membershipStatus", "MEMBER", "employmentStatus", "ACTIVE", "email", "excel@gpg.vn", "phone", "0901234567"));
        assertSuccessful("welfare", values(
                "recordCode", "XLS-WELFARE", "welfareType", "VISIT", "unitCode", "XLS-UNIT",
                "beneficiaryName", "Người thụ hưởng", "eventDate", "2026-03-01", "status", "COMPLETED",
                "amount", "250000", "documentStatus", "COMPLETE", "notes", "Nhập đủ trường từ Excel"));
        assertSuccessful("cases", values(
                "caseCode", "XLS-CASE", "receivedDate", "2026-03-02", "unitCode", "XLS-UNIT",
                "issueGroup", "Điều kiện làm việc", "severity", "HIGH", "ownerName", "PIC Excel",
                "deadline", "2026-03-20", "status", "IN_PROGRESS", "description", "Mô tả vụ việc",
                "affectedPeople", "4", "resultText", "Đang xử lý", "overdueReason", "Không quá hạn"));
        assertSuccessful("activities", values(
                "activityCode", "XLS-ACT", "name", "Hoạt động Excel", "unitCode", "XLS-UNIT",
                "eventDate", "2026-04-01", "status", "APPROVED", "objective", "Kết nối NLĐ",
                "plannedBudget", "3000000", "actualCost", "1500000", "participantCount", "25",
                "usefulnessScore", "4.5", "reportCompleted", "TRUE", "followUpOwner", "PIC Follow-up",
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
