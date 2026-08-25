package vn.gpg.unionportal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import vn.gpg.unionportal.service.ReportingService;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReportingServiceTests {
    @Autowired
    private ReportingService reportingService;

    @Test
    void dashboardCalculatesManualFinanceAndOperationalTotals() {
        var dashboard = reportingService.dashboard(YearMonth.of(2026, 8));

        assertThat(dashboard.unitCount()).isEqualTo(4);
        assertThat(dashboard.activeMemberCount()).isEqualTo(4);
        assertThat(dashboard.unionMemberCount()).isEqualTo(3);
        assertThat(dashboard.monthIncome()).isEqualByComparingTo(new BigDecimal("12000000"));
        assertThat(dashboard.monthExpense()).isEqualByComparingTo(new BigDecimal("6200000"));
        assertThat(dashboard.allTimeBalance()).isEqualByComparingTo(new BigDecimal("5800000"));
    }

    @Test
    void monthlyReportCanBeFilteredByUnionUnit() {
        var report = reportingService.monthlySummary(YearMonth.of(2026, 8), 2L);

        assertThat(report.unionUnitName()).isEqualTo("CĐCS GPL");
        assertThat(report.activeEmployees()).isEqualTo(1);
        assertThat(report.welfareCases()).isEqualTo(1);
        assertThat(report.activities()).isEqualTo(1);
        assertThat(report.expense()).isEqualByComparingTo(new BigDecimal("3200000"));
        assertThat(report.narrative()).isNotNull();
    }
}
