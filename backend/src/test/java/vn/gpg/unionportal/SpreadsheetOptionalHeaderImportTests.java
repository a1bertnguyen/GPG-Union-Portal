package vn.gpg.unionportal;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.model.DomainEnums.IntegrationStatus;
import vn.gpg.unionportal.service.SpreadsheetImportService;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SpreadsheetOptionalHeaderImportTests {

    @Autowired
    private SpreadsheetImportService service;

    @Test
    void importsOlderWorkbooksThatDoNotContainNewOptionalColumns() throws Exception {
        assertSuccessful("welfare", values(
                "recordCode", "LEGACY-WELFARE", "welfareType", "VISIT", "unitCode", "VCS",
                "beneficiaryName", "Đoàn viên cũ", "eventDate", "2026-08-20", "status", "NEW",
                "amount", "500000", "documentStatus", "INCOMPLETE"));

        assertSuccessful("cases", values(
                "caseCode", "LEGACY-CASE", "receivedDate", "2026-08-20", "unitCode", "VCS",
                "issueGroup", "Điều kiện làm việc", "severity", "MEDIUM", "ownerName", "PIC",
                "deadline", "2026-08-30", "status", "NEW", "description", "Vụ việc từ mẫu cũ",
                "affectedPeople", "1"));

        assertSuccessful("activities", values(
                "activityCode", "LEGACY-ACTIVITY", "name", "Chương trình từ mẫu cũ", "unitCode", "VCS",
                "eventDate", "2026-08-20", "status", "PLANNED", "plannedBudget", "1000000",
                "actualCost", "0", "participantCount", "10", "reportCompleted", "FALSE"));
    }

    private void assertSuccessful(String resource, Map<String, String> values) throws Exception {
        var result = service.importWorkbook(resource, workbook(resource, values));

        assertThat(result.errors()).as(resource).isEmpty();
        assertThat(result.createdRows()).as(resource).isEqualTo(1);
        assertThat(result.run().getStatus()).as(resource).isEqualTo(IntegrationStatus.COMPLETED);
    }

    private MockMultipartFile workbook(String resource, Map<String, String> values) throws Exception {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Dữ liệu");
            var header = sheet.createRow(0);
            var row = sheet.createRow(1);
            int columnIndex = 0;
            for (var entry : values.entrySet()) {
                header.createCell(columnIndex).setCellValue(entry.getKey());
                row.createCell(columnIndex).setCellValue(entry.getValue());
                columnIndex++;
            }
            workbook.write(output);
            return new MockMultipartFile("file", resource + "-legacy.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }

    private Map<String, String> values(String... pairs) {
        var result = new LinkedHashMap<String, String>();
        for (int index = 0; index < pairs.length; index += 2) {
            result.put(pairs[index], pairs[index + 1]);
        }
        return result;
    }
}
