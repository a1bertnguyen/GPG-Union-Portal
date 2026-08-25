package vn.gpg.unionportal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.model.DomainEnums.IntegrationStatus;
import vn.gpg.unionportal.model.DomainEnums.IntegrationType;
import vn.gpg.unionportal.repository.FinanceEntryRepository;
import vn.gpg.unionportal.repository.IntegrationRunRepository;
import vn.gpg.unionportal.service.DataIntegrationService;

import java.nio.charset.StandardCharsets;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DataIntegrationServiceTests {
    @Autowired
    private DataIntegrationService integrationService;

    @Autowired
    private FinanceEntryRepository financeRepository;

    @Autowired
    private IntegrationRunRepository runRepository;

    @Test
    void importsInternalFinanceCsvAndWritesAuditRun() {
        String csv = "entryCode,unitCode,transactionDate,entryType,category,amount,description,documentNumber,documentStatus\n"
                + "TC-INT-001,VCS,2026-08-20,EXPENSE,Đào tạo,1500000,Chi lớp kỹ năng,PC-INT-01,COMPLETE\n";
        var file = new MockMultipartFile("file", "finance.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        var result = integrationService.importFinance(file, "admin");

        assertThat(result.run().getIntegrationType()).isEqualTo(IntegrationType.FINANCE_IMPORT);
        assertThat(result.run().getStatus()).isEqualTo(IntegrationStatus.COMPLETED);
        assertThat(result.createdRows()).isEqualTo(1);
        assertThat(result.errors()).isEmpty();
        assertThat(financeRepository.findByEntryCodeIgnoreCase("TC-INT-001")).isPresent();
        assertThat(runRepository.findById(result.run().getId())).isPresent();

        String exported = new String(integrationService.exportFinance(YearMonth.of(2026, 8), 1L), StandardCharsets.UTF_8);
        assertThat(exported).contains("TC-INT-001", "PC-INT-01");
    }

    @Test
    void keepsValidRowsAndMarksRunPartialWhenFinanceCsvHasErrors() {
        String csv = "entryCode,unitCode,transactionDate,entryType,category,amount,description,documentNumber,documentStatus\n"
                + "TC-INT-002,VCS,2026-08-21,INCOME,Đoàn phí,500000,Thu bổ sung,PT-INT-02,COMPLETE\n"
                + "TC-INT-003,UNKNOWN,2026-08-21,EXPENSE,Khác,100000,Nội dung,,INCOMPLETE\n";
        var file = new MockMultipartFile("file", "finance-partial.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        var result = integrationService.importFinance(file, "admin");

        assertThat(result.run().getStatus()).isEqualTo(IntegrationStatus.PARTIAL);
        assertThat(result.run().getSuccessfulRows()).isEqualTo(1);
        assertThat(result.run().getFailedRows()).isEqualTo(1);
        assertThat(result.errors()).singleElement().asString().contains("UNKNOWN");
    }
}
