package vn.gpg.unionportal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import vn.gpg.unionportal.repository.AdminUserRepository;
import vn.gpg.unionportal.repository.MemberRepository;
import vn.gpg.unionportal.service.AuthService;
import vn.gpg.unionportal.dto.AuthModels.LoginRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthSecurityTests {
    @LocalServerPort
    private int port;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private MemberRepository memberRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @AfterEach
    void cleanCreatedMembers() {
        memberRepository.findByEmployeeCodeIgnoreCase("NV-USER-OWN").ifPresent(memberRepository::delete);
        memberRepository.findByEmployeeCodeIgnoreCase("NV-USER-CROSS").ifPresent(memberRepository::delete);
    }

    @Test
    void loginIssuesSignedAdminJwtAndUpdatesLastLogin() {
        var login = authService.login(new LoginRequest("ADMIN", "Admin@123!"));
        var jwt = jwtDecoder.decode(login.accessToken());

        assertThat(login.tokenType()).isEqualTo("Bearer");
        assertThat(login.user().role()).isEqualTo("ADMIN");
        assertThat(jwt.getSubject()).isEqualTo("admin");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN");
        assertThat(adminUserRepository.findByUsernameIgnoreCase("admin")).get()
                .extracting(admin -> admin.getLastLoginAt()).isNotNull();
    }

    @Test
    void rejectsInvalidPassword() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong-password")))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void protectsBusinessApiAndAcceptsBearerToken() throws Exception {
        var anonymousRequest = HttpRequest.newBuilder(uri("/api/units")).GET().build();
        var anonymousResponse = httpClient.send(anonymousRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(anonymousResponse.statusCode()).isEqualTo(401);

        String token = authService.login(new LoginRequest("admin", "Admin@123!")).accessToken();
        var adminRequest = HttpRequest.newBuilder(uri("/api/units"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        var adminResponse = httpClient.send(adminRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(adminResponse.statusCode()).isEqualTo(200);
        assertThat(adminResponse.body()).contains("CĐCS VCS");
    }

    @Test
    void userCanManageOwnUnitButCannotCrossScopeOrAccessAdministration() throws Exception {
        var login = authService.login(new LoginRequest("user.vcs", "User@123!"));
        var jwt = jwtDecoder.decode(login.accessToken());

        assertThat(login.user().role()).isEqualTo("USER");
        assertThat(login.user().unionUnitCode()).isEqualTo("VCS");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");
        assertThat(((Number) jwt.getClaim("unitId")).longValue()).isEqualTo(1L);

        var unitsRequest = HttpRequest.newBuilder(uri("/api/units"))
                .header("Authorization", "Bearer " + login.accessToken()).GET().build();
        var unitsResponse = httpClient.send(unitsRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(unitsResponse.statusCode()).isEqualTo(200);
        assertThat(unitsResponse.body()).contains("CĐCS VCS").doesNotContain("CĐCS GPL");

        var membersRequest = HttpRequest.newBuilder(uri("/api/members?unitId=2"))
                .header("Authorization", "Bearer " + login.accessToken()).GET().build();
        var membersResponse = httpClient.send(membersRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(membersResponse.statusCode()).isEqualTo(200);
        assertThat(membersResponse.body()).contains("NV3811").doesNotContain("NV2201");

        String ownMember = """
                {"employeeCode":"NV-USER-OWN","fullName":"Đoàn viên USER","unionUnitId":1,
                 "jobTitle":"Nhân viên","workplace":"VP-TCT","joinDate":"2026-08-01",
                 "membershipStatus":"MEMBER","employmentStatus":"ACTIVE",
                 "email":"user.own@gpg.vn","phone":"0909000001"}
                """;
        var createRequest = HttpRequest.newBuilder(uri("/api/members"))
                .header("Authorization", "Bearer " + login.accessToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(ownMember))
                .build();
        var createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(createResponse.statusCode()).isEqualTo(201);

        String otherUnitMember = ownMember.replace("NV-USER-OWN", "NV-USER-CROSS").replace("\"unionUnitId\":1", "\"unionUnitId\":2");
        var crossScopeRequest = HttpRequest.newBuilder(uri("/api/members"))
                .header("Authorization", "Bearer " + login.accessToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(otherUnitMember))
                .build();
        var crossScopeResponse = httpClient.send(crossScopeRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(crossScopeResponse.statusCode()).isEqualTo(403);

        var administrationRequest = HttpRequest.newBuilder(uri("/api/admin/users"))
                .header("Authorization", "Bearer " + login.accessToken()).GET().build();
        var administrationResponse = httpClient.send(administrationRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(administrationResponse.statusCode()).isEqualTo(403);

        var memberTemplateRequest = HttpRequest.newBuilder(uri("/api/spreadsheets/members/template.xlsx"))
                .header("Authorization", "Bearer " + login.accessToken()).GET().build();
        var memberTemplateResponse = httpClient.send(memberTemplateRequest, HttpResponse.BodyHandlers.ofByteArray());
        assertThat(memberTemplateResponse.statusCode()).isEqualTo(200);
        assertThat(memberTemplateResponse.headers().firstValue("Content-Type")).hasValueSatisfying(value ->
                assertThat(value).contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        var unitTemplateRequest = HttpRequest.newBuilder(uri("/api/spreadsheets/units/template.xlsx"))
                .header("Authorization", "Bearer " + login.accessToken()).GET().build();
        var unitTemplateResponse = httpClient.send(unitTemplateRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(unitTemplateResponse.statusCode()).isEqualTo(403);
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
