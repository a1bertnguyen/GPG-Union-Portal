package vn.gpg.unionportal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.flywaydb.core.Flyway;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.regex.Pattern;

/** Test-only translation of the MySQL V16 procedure; production migrations/checksums are untouched. */
@Configuration
public class H2MigrationConfiguration {
    @Bean
    FlywayMigrationStrategy h2MigrationStrategy() {
        return flyway -> {
            try (var connection = flyway.getConfiguration().getDataSource().getConnection()) {
                if (!connection.getMetaData().getURL().startsWith("jdbc:h2:")) { flyway.migrate(); return; }
            } catch (java.sql.SQLException e) { throw new IllegalStateException(e); }
            Path directory = null;
            var files = new ArrayList<Path>();
            try {
                directory = Files.createTempDirectory("union-h2-migrations-");
                for (var resource : new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/V*.sql")) {
                    String name = resource.getFilename();
                    String sql = resource.getContentAsString(StandardCharsets.UTF_8);
                    if ("V16__add_activity_program_reports.sql".equals(name)) {
                        var matcher = Pattern.compile("CALL gpg_v16_add_column_if_missing\\('([^']+)', '([^']+)'\\);").matcher(sql);
                        StringBuilder portable = new StringBuilder();
                        while (matcher.find()) portable.append("ALTER TABLE union_activities ADD COLUMN IF NOT EXISTS ")
                            .append(matcher.group(1)).append(' ').append(matcher.group(2)).append("; ");
                        if (portable.isEmpty()) throw new IllegalStateException("V16 test adapter did not find column declarations");
                        sql = portable.toString();
                    }
                    Path target = directory.resolve(name);
                    Files.writeString(target, sql);
                    files.add(target);
                }
                Flyway.configure().configuration(flyway.getConfiguration()).locations("filesystem:" + directory.toAbsolutePath()).load().migrate();
            } catch (java.io.IOException e) { throw new IllegalStateException(e); }
            finally {
                for (Path file : files) try { Files.deleteIfExists(file); } catch (java.io.IOException ignored) { }
                if (directory != null) try { Files.deleteIfExists(directory); } catch (java.io.IOException ignored) { }
            }
        };
    }
}
