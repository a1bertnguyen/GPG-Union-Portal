package vn.gpg.unionportal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.controller.FinanceDocumentController;
import vn.gpg.unionportal.dto.ApiModels.FinanceRequest;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.DomainEnums.DocumentStatus;
import vn.gpg.unionportal.model.DomainEnums.FinanceEntryType;
import vn.gpg.unionportal.repository.FinanceEntryRepository;
import vn.gpg.unionportal.repository.UnionUnitRepository;
import vn.gpg.unionportal.service.FinanceDocumentService;
import vn.gpg.unionportal.service.FinanceService;
import vn.gpg.unionportal.service.ReportingService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class FinanceAdvanceAndDocumentsTests {
    @Autowired private FinanceService finance;
    @Autowired private FinanceDocumentService documents;
    @Autowired private ReportingService reporting;
    @Autowired private FinanceEntryRepository entries;
    @Autowired private UnionUnitRepository units;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void advanceIsTrackedSeparatelyFromIncomeAndExpense() {
        var unit = units.findByCodeIgnoreCase("VCS").orElseThrow();
        authenticateUser(unit.getId());
        var before = finance.facets(ListQuery.firstPage()).metrics();
        var reportBefore = reporting.financeSummary(YearMonth.now(), unit.getId());

        finance.create(request(unit.getId(), FinanceEntryType.ADVANCE));

        var after = finance.facets(ListQuery.firstPage()).metrics();
        var reportAfter = reporting.financeSummary(YearMonth.now(), unit.getId());
        assertThat(decimal(after.get("advance")).subtract(decimal(before.get("advance"))))
                .isEqualByComparingTo("750000");
        assertThat(decimal(after.get("income"))).isEqualByComparingTo(decimal(before.get("income")));
        assertThat(decimal(after.get("expense")).subtract(decimal(before.get("expense"))))
                .isEqualByComparingTo("750000");
        assertThat(decimal(after.get("balance")).subtract(decimal(before.get("balance"))))
                .isEqualByComparingTo("-750000");
        assertThat(reportAfter.advance().subtract(reportBefore.advance())).isEqualByComparingTo("750000");
        assertThat(reportAfter.expense().subtract(reportBefore.expense())).isEqualByComparingTo("750000");
        assertThat(reportAfter.balance().subtract(reportBefore.balance())).isEqualByComparingTo("-750000");
    }

    @Test
    void documentUploadIsBoundToOneVoucherAndUpdatesItsStatus() throws Exception {
        var unit = units.findByCodeIgnoreCase("VCS").orElseThrow();
        authenticateUser(unit.getId());
        var entry = finance.create(request(unit.getId(), FinanceEntryType.EXPENSE));
        var file = new MockMultipartFile("file", "hoa-don.pdf", "application/pdf",
                "noi-dung-chung-tu".getBytes(StandardCharsets.UTF_8));

        var uploaded = documents.upload(entry.getId(), file);

        assertThat(uploaded.financeEntryId()).isEqualTo(entry.getId());
        assertThat(uploaded.entryCode()).isEqualTo(entry.getEntryCode());
        assertThat(documents.list(entry.getId())).singleElement()
                .extracting(document -> document.fileName()).isEqualTo("hoa-don.pdf");
        assertThat(documents.download(uploaded.id()).data()).isEqualTo(file.getBytes());
        assertThat(entries.findById(entry.getId()).orElseThrow().getDocumentStatus())
                .isEqualTo(DocumentStatus.COMPLETE);

        documents.delete(uploaded.id());
        assertThat(documents.list(entry.getId())).isEmpty();
        assertThat(entries.findById(entry.getId()).orElseThrow().getDocumentStatus())
                .isEqualTo(DocumentStatus.INCOMPLETE);
    }

    @Test
    void financeDocumentUploadRouteBindsBrowserFormData() throws Exception {
        FinanceDocumentService service = mock(FinanceDocumentService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FinanceDocumentController(service)).build();
        var file = new MockMultipartFile("file", "hoa-don.pdf", "application/pdf", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/finance-documents")
                        .file(file)
                        .param("financeEntryId", "42"))
                .andExpect(status().isCreated());

        verify(service).upload(eq(42L), any(MultipartFile.class));
    }

    private FinanceRequest request(Long unitId, FinanceEntryType type) {
        return new FinanceRequest(
                "FIN-" + UUID.randomUUID().toString().substring(0, 8),
                unitId,
                LocalDate.now(),
                type,
                type == FinanceEntryType.ADVANCE ? "Tạm ứng chương trình" : "Chi hoạt động",
                new BigDecimal("750000"),
                "Phiếu kiểm thử chứng từ",
                null,
                DocumentStatus.INCOMPLETE);
    }

    private BigDecimal decimal(Number value) {
        return new BigDecimal(value.toString());
    }

    private void authenticateUser(Long unitId) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("finance-user-token")
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
