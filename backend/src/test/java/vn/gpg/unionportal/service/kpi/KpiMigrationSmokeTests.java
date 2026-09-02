package vn.gpg.unionportal.service.kpi;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class KpiMigrationSmokeTests {
    @Test
    void createsTheKpiCatalogAndAllThirtyOneDefinitions(@TempDir Path migrationDirectory) throws Exception {
        String url = "jdbc:h2:mem:kpi_v21;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE union_units(id BIGINT AUTO_INCREMENT PRIMARY KEY)");
        }

        Files.copy(Path.of("src/main/resources/db/migration/V21__create_gpg_kpi_engine.sql"),
                migrationDirectory.resolve("V21__create_gpg_kpi_engine.sql"));
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("filesystem:" + migrationDirectory.toAbsolutePath().toString().replace('\\', '/'))
                .baselineOnMigrate(true)
                .baselineVersion("20")
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT COUNT(*), SUM(weight), SUM(CASE
                         WHEN direction = 'HIGHER_BETTER' AND (target_value IS NULL OR target_value <= 0) THEN 1
                         WHEN direction = 'LOWER_BETTER' AND (target_value IS NULL OR max_allowed_value IS NULL
                              OR max_allowed_value <= target_value) THEN 1
                         ELSE 0 END)
                     FROM kpi_definitions
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(31);
            assertThat(result.getBigDecimal(2)).isEqualByComparingTo("100");
            assertThat(result.getInt(3)).isZero();
        }

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var columns = connection.getMetaData().getColumns(null, null,
                     "kpi_no_occurrence_confirmations", "reconciliation_source_module")) {
            assertThat(columns.next()).isTrue();
        }

        for (String column : new String[]{"evidence_module", "evidence_record_id",
                "effectiveness_verified", "non_duplicate_verified"}) {
            try (var connection = DriverManager.getConnection(url, "sa", "");
                 var columns = connection.getMetaData().getColumns(null, null, "kpi_adjustments", column)) {
                assertThat(columns.next()).as("kpi_adjustments.%s", column).isTrue();
            }
        }

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT COUNT(*) FROM kpi_source_exclusions
                     WHERE source_module = 'DM_CONG_DOAN' AND source_record_key IN ('VCS','GPL','AZC','GPD')
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(4);
        }
    }
}
