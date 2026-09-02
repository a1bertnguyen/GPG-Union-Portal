package vn.gpg.unionportal;

import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import vn.gpg.unionportal.repository.UnionActivityRepository;
import vn.gpg.unionportal.service.ReportExcelService;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReportExcelServiceTests {
    @Autowired
    private ReportExcelService service;

    @Autowired
    private UnionActivityRepository activityRepository;

    @Test
    void exportsThePeriodicAndCompanySummaryWorkbooksWithSourceDetailSheets() throws Exception {
        try (var periodic = WorkbookFactory.create(new ByteArrayInputStream(service.exportPeriodicReport("2026-08", null)));
             var company = WorkbookFactory.create(new ByteArrayInputStream(service.exportCompanySummary("2026-08", null)))) {
            assertThat(periodic.getSheet("Tổng hợp").getRow(0).getCell(0).getStringCellValue())
                    .contains("BÁO CÁO CÔNG ĐOÀN ĐỊNH KỲ");
            assertThat(periodic.getSheet("Báo cáo định kỳ")).isNotNull();
            assertThat(periodic.getSheet("Chương trình")).isNotNull();
            assertThat(periodic.getSheet("Chăm lo NLĐ")).isNotNull();
            assertThat(periodic.getSheet("Kiến nghị NLĐ")).isNotNull();
            assertThat(periodic.getSheet("Tài chính")).isNotNull();
            assertThat(company.getSheet("Tổng hợp").getRow(0).getCell(0).getStringCellValue())
                    .contains("BÁO CÁO TỔNG HỢP CÔNG ĐOÀN CÔNG TY");
        }
    }

    @Test
    void exportsAnActivityReportAndCaseBookAsReadableWorkbooks() throws Exception {
        Long activityId = activityRepository.findAll().getFirst().getId();

        try (var activity = WorkbookFactory.create(new ByteArrayInputStream(service.exportActivityReport(activityId)));
             var cases = WorkbookFactory.create(new ByteArrayInputStream(service.exportCaseBook()))) {
            assertThat(activity.getSheet("Báo cáo sau CT").getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo("BÁO CÁO SAU CHƯƠNG TRÌNH");
            assertThat(activity.getSheet("Hồ sơ đính kèm")).isNotNull();
            assertThat(cases.getSheet("Sổ kiến nghị").getRow(0).getCell(0).getStringCellValue())
                    .contains("SỔ KIẾN NGHỊ");
        }
    }
}
