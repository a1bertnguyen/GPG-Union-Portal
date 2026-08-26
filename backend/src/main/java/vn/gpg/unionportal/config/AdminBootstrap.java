package vn.gpg.unionportal.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.model.AdminUser;
import vn.gpg.unionportal.repository.AdminUserRepository;
import vn.gpg.unionportal.repository.UnionUnitRepository;

import java.util.Locale;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdminUserRepository repository;
    private final UnionUnitRepository unitRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String adminFullName;
    private final String userUsername;
    private final String userPassword;
    private final String userFullName;
    private final String userUnitCode;
    private final boolean bootstrapUserEnabled;

    public AdminBootstrap(AdminUserRepository repository,
                          UnionUnitRepository unitRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.auth.bootstrap-admin.username}") String adminUsername,
                          @Value("${app.auth.bootstrap-admin.password}") String adminPassword,
                          @Value("${app.auth.bootstrap-admin.full-name}") String adminFullName,
                          @Value("${app.auth.bootstrap-user.username}") String userUsername,
                          @Value("${app.auth.bootstrap-user.password}") String userPassword,
                          @Value("${app.auth.bootstrap-user.full-name}") String userFullName,
                          @Value("${app.auth.bootstrap-user.unit-code}") String userUnitCode,
                          @Value("${app.auth.bootstrap-user.enabled:false}") boolean bootstrapUserEnabled) {
        this.repository = repository;
        this.unitRepository = unitRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminFullName = adminFullName;
        this.userUsername = userUsername;
        this.userPassword = userPassword;
        this.userFullName = userFullName;
        this.userUnitCode = userUnitCode;
        this.bootstrapUserEnabled = bootstrapUserEnabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createAdmin();
        if (bootstrapUserEnabled) createUser();
    }

    private void createAdmin() {
        String normalizedUsername = adminUsername.trim().toLowerCase(Locale.ROOT);
        if (repository.findByUsernameIgnoreCase(normalizedUsername).isPresent()) return;

        var account = account(normalizedUsername, adminPassword, adminFullName, "ADMIN");
        repository.save(account);
        log.info("Bootstrap ADMIN account initialized.");
    }

    private void createUser() {
        String normalizedUsername = userUsername.trim().toLowerCase(Locale.ROOT);
        if (repository.findByUsernameIgnoreCase(normalizedUsername).isPresent()) return;

        var unit = unitRepository.findByCodeIgnoreCase(userUnitCode.trim())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy CĐCS cho tài khoản USER: " + userUnitCode));
        var account = account(normalizedUsername, userPassword, userFullName, "USER");
        account.setUnionUnit(unit);
        repository.save(account);
        log.info("Bootstrap USER account initialized for unit '{}'.", unit.getCode());
    }

    private AdminUser account(String username, String password, String fullName, String role) {
        var account = new AdminUser();
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setFullName(fullName.trim());
        account.setRole(role);
        account.setActive(true);
        return account;
    }
}
