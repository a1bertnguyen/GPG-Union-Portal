package vn.gpg.unionportal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.repository.UnionUnitRepository;
import vn.gpg.unionportal.service.DocumentLibraryService;
import vn.gpg.unionportal.service.KpiService;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentLibraryAndKpiTests {
    @Autowired private UnionUnitRepository units;
    @Autowired private DocumentLibraryService documents;
    @Autowired private KpiService kpi;
    @Autowired private MockMvc mockMvc;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminDistributesDocumentsAndUsersOnlyReadTheirOwnUnit() {
        var vcs = units.findByCodeIgnoreCase("VCS").orElseThrow();
        var gpl = units.findByCodeIgnoreCase("GPL").orElseThrow();
        authenticateAdmin();
        var vcsDocument = documents.upload(vcs.getId(), "Hướng dẫn", "Quy trình chăm lo", "Áp dụng nội bộ",
                file("quy-trinh.pdf"));
        var gplDocument = documents.upload(gpl.getId(), "Biểu mẫu", "Biểu mẫu GPL", null,
                file("bieu-mau.pdf"));

        authenticateUser(vcs.getId());
        assertThat(documents.search(ListQuery.allForUnit(gpl.getId())))
                .extracting("id").containsExactly(vcsDocument.id());
        assertThat(documents.download(vcsDocument.id()).data()).containsExactly(1, 2, 3);
        assertThatThrownBy(() -> documents.download(gplDocument.id())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> documents.upload(vcs.getId(), "Khác", "Không được phép", null,
                file("blocked.pdf"))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void kpiResultsAreScopedToTheUsersAssignedUnit() {
        var vcs = units.findByCodeIgnoreCase("VCS").orElseThrow();
        var gpl = units.findByCodeIgnoreCase("GPL").orElseThrow();
        authenticateUser(vcs.getId());

        var result = kpi.evaluate(YearMonth.of(2026, 8), gpl.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().unionUnitId()).isEqualTo(vcs.getId());
        assertThat(result.getFirst().criteria()).hasSize(10);
        assertThat(result.getFirst().score()).isBetween(0, 100);
    }

    @Test
    void userCannotCreateWelfarePoliciesOrUploadLibraryDocuments() throws Exception {
        mockMvc.perform(post("/api/welfare-policies").with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/document-library").with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    private MockMultipartFile file(String name) {
        return new MockMultipartFile("file", name, "application/pdf", new byte[]{1, 2, 3});
    }

    private void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private void authenticateUser(Long unitId) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("document-user-token")
                .header("alg", "none")
                .subject("user.vcs")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("unitId", unitId)
                .claim("roles", List.of("USER"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")), "user.vcs"));
    }
}
