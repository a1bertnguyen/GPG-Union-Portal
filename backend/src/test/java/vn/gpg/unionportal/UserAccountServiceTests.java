package vn.gpg.unionportal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.service.AuthService;
import vn.gpg.unionportal.service.UserAccountService;
import vn.gpg.unionportal.dto.AuthModels.LoginRequest;
import vn.gpg.unionportal.dto.UserAccountModels.UserAccountRequest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserAccountServiceTests {
    @Autowired
    private UserAccountService accountService;

    @Autowired
    private AuthService authService;

    @Test
    void adminCanCreateUserBoundToAUnionUnit() {
        var created = accountService.create(new UserAccountRequest(
                "user.gpl.test", "Người dùng GPL", "USER", 2L, true, "Strong@123"));

        assertThat(created.role()).isEqualTo("USER");
        assertThat(created.unionUnitCode()).isEqualTo("GPL");

        var login = authService.login(new LoginRequest("user.gpl.test", "Strong@123"));
        assertThat(login.user().role()).isEqualTo("USER");
        assertThat(login.user().unionUnitId()).isEqualTo(2L);
    }

    @Test
    void adminAccountAlwaysHasSystemWideScope() {
        var created = accountService.create(new UserAccountRequest(
                "admin.test", "Quản trị kiểm thử", "ADMIN", 2L, true, "Strong@123"));

        assertThat(created.role()).isEqualTo("ADMIN");
        assertThat(created.unionUnitId()).isNull();
    }
}
