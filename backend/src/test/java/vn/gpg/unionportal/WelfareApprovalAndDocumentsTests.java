package vn.gpg.unionportal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.WelfareRequest;
import vn.gpg.unionportal.model.DomainEnums.DocumentStatus;
import vn.gpg.unionportal.model.DomainEnums.FinanceEntryType;
import vn.gpg.unionportal.model.DomainEnums.WelfareDocumentType;
import vn.gpg.unionportal.model.DomainEnums.WelfareType;
import vn.gpg.unionportal.model.DomainEnums.WorkStatus;
import vn.gpg.unionportal.repository.UnionUnitRepository;
import vn.gpg.unionportal.repository.FinanceEntryRepository;
import vn.gpg.unionportal.repository.WelfarePolicyRepository;
import vn.gpg.unionportal.repository.WelfareRecordRepository;
import vn.gpg.unionportal.service.WelfareDocumentService;
import vn.gpg.unionportal.service.WelfareService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class WelfareApprovalAndDocumentsTests {
    @Autowired private UnionUnitRepository units;
    @Autowired private WelfarePolicyRepository policies;
    @Autowired private WelfareRecordRepository records;
    @Autowired private WelfareService welfare;
    @Autowired private WelfareDocumentService documents;
    @Autowired private FinanceEntryRepository financeEntries;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userRequestWaitsForAdminApprovalAndUserCannotApproveIt() {
        var unit = units.findByCodeIgnoreCase("VCS").orElseThrow();
        var policy = policies.findByCodeIgnoreCase("CD-01-01").orElseThrow();
        authenticateUser(unit.getId());

        var created = welfare.create(request("APPROVAL", unit.getId(), policy.getId(), WorkStatus.COMPLETED));

        assertThat(created.getStatus()).isEqualTo(WorkStatus.PENDING_APPROVAL);
        assertThat(created.getDocumentStatus()).isEqualTo(DocumentStatus.INCOMPLETE);
        assertThat(created.getReceiptStatus()).isEqualTo(DocumentStatus.INCOMPLETE);
        assertThat(created.getHasImage()).isFalse();
        assertThatThrownBy(() -> welfare.approve(created.getId())).isInstanceOf(AccessDeniedException.class);

        authenticateAdmin();
        var approved = welfare.approve(created.getId());
        assertThat(approved.getStatus()).isEqualTo(WorkStatus.IN_PROGRESS);
        var payment = financeEntries.findByEntryCodeIgnoreCase("PC-CL-" + created.getId()).orElseThrow();
        assertThat(payment.getEntryType()).isEqualTo(FinanceEntryType.EXPENSE);
        assertThat(payment.getAmount()).isEqualByComparingTo(policy.getSupportAmount());
        assertThat(payment.getUnionUnit().getId()).isEqualTo(unit.getId());
        assertThat(payment.getDocumentNumber()).isEqualTo(created.getRecordCode());
        assertThat(payment.getDocumentStatus()).isEqualTo(DocumentStatus.INCOMPLETE);

        authenticateUser(unit.getId());
        assertThatThrownBy(() -> welfare.update(created.getId(), request(
                created.getRecordCode(), unit.getId(), policy.getId(), WorkStatus.PENDING_APPROVAL)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void uploadedFilesAutomaticallyRefreshWelfareDocumentFlags() {
        var unit = units.findByCodeIgnoreCase("VCS").orElseThrow();
        var policy = policies.findByCodeIgnoreCase("CD-01-01").orElseThrow();
        authenticateUser(unit.getId());
        var created = welfare.create(request("DOCUMENTS", unit.getId(), policy.getId(), WorkStatus.NEW));

        var supporting = documents.upload(created.getId(), WelfareDocumentType.SUPPORTING_DOCUMENT,
                file("ho-so.pdf", "application/pdf"));
        assertState(created.getId(), DocumentStatus.COMPLETE, DocumentStatus.INCOMPLETE, false);

        documents.upload(created.getId(), WelfareDocumentType.RECEIPT,
                file("bien-nhan.pdf", "application/pdf"));
        assertState(created.getId(), DocumentStatus.COMPLETE, DocumentStatus.COMPLETE, false);

        documents.upload(created.getId(), WelfareDocumentType.IMAGE,
                file("hinh-anh.png", "image/png"));
        assertState(created.getId(), DocumentStatus.COMPLETE, DocumentStatus.COMPLETE, true);

        assertThat(documents.list(created.getId())).hasSize(3);
        documents.delete(supporting.id());
        assertState(created.getId(), DocumentStatus.INCOMPLETE, DocumentStatus.COMPLETE, true);
    }

    private WelfareRequest request(String suffix, Long unitId, Long policyId, WorkStatus status) {
        return new WelfareRequest(
                "WF-" + suffix + "-" + UUID.randomUUID().toString().substring(0, 8),
                WelfareType.HARDSHIP, "Giá trị phía client", unitId, "Người thụ hưởng",
                LocalDate.of(2026, 8, 28), LocalDate.of(2030, 1, 1), status,
                new BigDecimal("300000"), BigDecimal.ZERO, DocumentStatus.COMPLETE,
                DocumentStatus.COMPLETE, true, null, policyId);
    }

    private MockMultipartFile file(String name, String contentType) {
        return new MockMultipartFile("file", name, contentType, new byte[]{1, 2, 3});
    }

    private void assertState(Long recordId, DocumentStatus documentStatus,
                             DocumentStatus receiptStatus, boolean hasImage) {
        var record = records.findById(recordId).orElseThrow();
        assertThat(record.getDocumentStatus()).isEqualTo(documentStatus);
        assertThat(record.getReceiptStatus()).isEqualTo(receiptStatus);
        assertThat(record.getHasImage()).isEqualTo(hasImage);
    }

    private void authenticateUser(Long unitId) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("welfare-user-token")
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

    private void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}
