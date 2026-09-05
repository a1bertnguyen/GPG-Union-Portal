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

    @Test
    void v22ReplacesTheActiveCatalogWithTwentyThreeComputableCodes(@TempDir Path migrationDirectory)
            throws Exception {
        String url = "jdbc:h2:mem:kpi_v22;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE union_units(id BIGINT AUTO_INCREMENT PRIMARY KEY)");
            statement.execute("CREATE TABLE welfare_records(id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "status VARCHAR(30) NOT NULL, updated_at TIMESTAMP NOT NULL)");
        }

        for (String migration : new String[]{"V21__create_gpg_kpi_engine.sql",
                "V22__add_gpg_kpi_v2_catalog.sql"}) {
            Files.copy(Path.of("src/main/resources/db/migration/" + migration),
                    migrationDirectory.resolve(migration));
        }
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
                     FROM kpi_definitions WHERE version_id = 'GPG-CD-KPI-V2'
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(23);
            assertThat(result.getBigDecimal(2)).isEqualByComparingTo("100");
            assertThat(result.getInt(3)).isZero();
        }

        // The retired V1 catalog must not be selectable any more, or two versions would cover the same period.
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery(
                     "SELECT status FROM kpi_versions WHERE version_id = 'GPG-CD-KPI-V1'")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString(1)).isEqualTo("SUPERSEDED");
        }

        // REP01 and the P01 penalty stay dead until the submission SLA exists for the active version.
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT duration_value, duration_unit FROM sla_rules
                     WHERE version_id = 'GPG-CD-KPI-V2' AND sla_code = 'REPORT_SUBMISSION'
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(5);
            assertThat(result.getString(2)).isEqualTo("BUSINESS_DAY");
        }

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var columns = connection.getMetaData().getColumns(null, null, "welfare_records", "completed_at")) {
            assertThat(columns.next()).isTrue();
        }
    }
}
