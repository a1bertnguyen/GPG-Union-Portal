package vn.gpg.unionportal;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import vn.gpg.unionportal.service.ReportingService;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class DashboardQueryCountTests {
    @Autowired private ReportingService reportingService;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @Test
    void executiveDashboardUsesBoundedAggregateQueries() {
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        reportingService.dashboard(YearMonth.of(2026, 8));

        assertThat(statistics.getPrepareStatementCount())
                .as("dashboard query count")
                .isBetween(1L, 7L);
    }
}
