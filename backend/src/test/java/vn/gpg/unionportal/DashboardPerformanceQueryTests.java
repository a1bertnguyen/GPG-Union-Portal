package vn.gpg.unionportal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.service.ActivityService;
import vn.gpg.unionportal.service.FinanceService;
import vn.gpg.unionportal.service.LaborCaseService;
import vn.gpg.unionportal.service.WelfareService;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class DashboardPerformanceQueryTests {
    @Autowired private WelfareService welfare;
    @Autowired private LaborCaseService cases;
    @Autowired private ActivityService activities;
    @Autowired private FinanceService finance;

    @Test
    void dashboardListQueriesOnlyReturnRowsFromTheRequestedMonth() {
        YearMonth expected = YearMonth.of(2026, 8);
        ListQuery query = dashboardQuery(expected.toString());

        assertThat(welfare.search(query)).isNotEmpty()
                .allMatch(item -> YearMonth.from(item.getEventDate()).equals(expected));
        assertThat(cases.search(query)).isNotEmpty()
                .allMatch(item -> YearMonth.from(item.getReceivedDate()).equals(expected));
        assertThat(activities.search(query)).isNotEmpty()
                .allMatch(item -> YearMonth.from(item.getEventDate()).equals(expected));
        assertThat(finance.search(query)).isNotEmpty()
                .allMatch(item -> YearMonth.from(item.getTransactionDate()).equals(expected));
    }

    @Test
    void unpagedCopiesKeepTheDashboardMonthFilter() {
        ListQuery query = dashboardQuery("2026-08").withoutPaging();

        assertThat(query.fetchAll()).isTrue();
        assertThat(query.monthValue()).isEqualTo(YearMonth.of(2026, 8));
    }

    @Test
    void invalidDashboardMonthIsRejected() {
        assertThatThrownBy(() -> dashboardQuery("08-2026").monthValue())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yyyy-MM");
    }

    private static ListQuery dashboardQuery(String month) {
        return new ListQuery(null, null, true, null, null, null, null, null, month);
    }
}
