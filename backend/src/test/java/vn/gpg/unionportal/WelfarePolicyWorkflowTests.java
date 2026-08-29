package vn.gpg.unionportal;

import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.WelfareRequest;
import vn.gpg.unionportal.model.DomainEnums.DocumentStatus;
import vn.gpg.unionportal.model.DomainEnums.WelfareType;
import vn.gpg.unionportal.model.DomainEnums.WorkStatus;
import vn.gpg.unionportal.repository.UnionUnitRepository;
import vn.gpg.unionportal.repository.WelfarePolicyRepository;
import vn.gpg.unionportal.service.WelfarePolicyExcelService;
import vn.gpg.unionportal.service.WelfareService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class WelfarePolicyWorkflowTests {
    @Autowired private WelfarePolicyRepository policyRepository;
    @Autowired private UnionUnitRepository unitRepository;
    @Autowired private WelfarePolicyExcelService excelService;
    @Autowired private WelfareService welfareService;

    @Test
    void migrationSeedsAttachedPolicyCatalogAndExportKeepsRoundTripColumns() throws Exception {
        assertThat(policyRepository.count()).isGreaterThanOrEqualTo(17);
        assertThat(policyRepository.findByCodeIgnoreCase("CD-01-01")).isPresent();
        assertThat(policyRepository.findAll())
                .allMatch(policy -> policy.getProcessingWeeks() >= 1 && policy.getProcessingWeeks() <= 8);

        byte[] exported = excelService.exportWorkbook();
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(exported))) {
            var sheet = workbook.getSheet("Quy Định Chế Độ Hỗ Trợ");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("TT");
            assertThat(sheet.getRow(3).getCell(4).getStringCellValue()).contains("THỜI HẠN XỬ LÝ");
            assertThat(sheet.getRow(3).getCell(5).getStringCellValue()).isEqualTo("MÃ CHÍNH SÁCH");
        }
    }

    @Test
    void importsOriginalFourColumnLayoutAndDefaultsProcessingTimeToOneWeek() throws Exception {
        byte[] input;
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Quy Định Chế Độ Hỗ Trợ");
            sheet.createRow(0).createCell(0).setCellValue("BẢNG QUY ĐỊNH CHẾ ĐỘ HỖ TRỢ PHÚC LỢI");
            var header = sheet.createRow(3);
            header.createCell(0).setCellValue("TT");
            header.createCell(1).setCellValue("NỘI DUNG");
            header.createCell(2).setCellValue("HỖ TRỢ\n(VNĐ)");
            header.createCell(3).setCellValue("GHI CHÚ");
            sheet.createRow(4).createCell(0).setCellValue("I. Công Đoàn Hỗ trợ:");
            var row = sheet.createRow(5);
            row.createCell(0).setCellValue(77);
            row.createCell(1).setCellValue("Sinh Nhật kiểm thử");
            row.createCell(2).setCellValue(123000);
            row.createCell(3).setCellValue("Tất cả nhân viên");
            workbook.write(output);
            input = output.toByteArray();
        }

        var file = new MockMultipartFile("file", "Bang_Che_Do_Ho_Tro_Phuc_Loi.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", input);
        var result = excelService.importWorkbook(file);

        assertThat(result.errors()).isEmpty();
        assertThat(result.createdRows()).isEqualTo(1);
        var imported = policyRepository.findByCodeIgnoreCase("CD-77-01").orElseThrow();
        assertThat(imported.getWelfareType()).isEqualTo(WelfareType.BIRTHDAY);
        assertThat(imported.getProcessingWeeks()).isEqualTo(1);
        assertThat(imported.getSupportAmount()).isEqualByComparingTo("123000");
    }

    @Test
    void selectedPolicyOverridesSnapshotsAndCalculatesDeadlineFromDetectionDate() {
        var policy = policyRepository.findByCodeIgnoreCase("CD-01-01").orElseThrow();
        policy.setProcessingWeeks(8);
        policyRepository.save(policy);
        var unit = unitRepository.findByCodeIgnoreCase("VCS").orElseThrow();
        LocalDate detectedOn = LocalDate.of(2026, 8, 1);

        var saved = welfareService.create(new WelfareRequest(
                "POLICY-DEADLINE-TEST", WelfareType.HARDSHIP, "Sai chính sách", unit.getId(),
                "Người thụ hưởng", detectedOn, LocalDate.of(2030, 1, 1), WorkStatus.NEW,
                new BigDecimal("300000"), BigDecimal.ZERO, DocumentStatus.INCOMPLETE,
                DocumentStatus.INCOMPLETE, false, null, policy.getId()));

        assertThat(saved.getPolicyId()).isEqualTo(policy.getId());
        assertThat(saved.getPolicyName()).isEqualTo(policy.getName());
        assertThat(saved.getWelfareType()).isEqualTo(policy.getWelfareType());
        assertThat(saved.getStandardAmount()).isEqualByComparingTo(policy.getSupportAmount());
        assertThat(saved.getDeadline()).isEqualTo(detectedOn.plusWeeks(8));
    }
}
