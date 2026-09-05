package vn.gpg.unionportal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.InventoryModels.GiftIssueRequest;
import vn.gpg.unionportal.dto.InventoryModels.ItemRequest;
import vn.gpg.unionportal.dto.InventoryModels.ReceiptRequest;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.DomainEnums.EmploymentStatus;
import vn.gpg.unionportal.model.DomainEnums.MembershipStatus;
import vn.gpg.unionportal.model.Member;
import vn.gpg.unionportal.model.UnionUnit;
import vn.gpg.unionportal.repository.MemberRepository;
import vn.gpg.unionportal.repository.UnionUnitRepository;
import vn.gpg.unionportal.service.InventoryService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class InventoryServiceTests {
    @Autowired private InventoryService inventory;
    @Autowired private UnionUnitRepository units;
    @Autowired private MemberRepository members;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void derivesStockFromSlipsAndRejectsAChangeThatWouldMakeItNegative() {
        UnionUnit unit = units.findByCodeIgnoreCase("VCS").orElseThrow();
        authenticateUser(unit.getId());
        var item = inventory.createItem(itemRequest(unit.getId(), "STOCK"));
        var receipt = inventory.createReceipt(new ReceiptRequest(unit.getId(), item.id(), LocalDate.now(), 5,
                "Nhà cung cấp kiểm thử", "PN-01", null));
        Member recipient = member(unit, "REC-STOCK", MembershipStatus.MEMBER, EmploymentStatus.ACTIVE);

        inventory.createIssue(new GiftIssueRequest(unit.getId(), item.id(), recipient.getId(), LocalDate.now(), 3,
                "Sinh nhật", "PX-01", null));

        var balance = inventory.getItem(item.id());
        assertThat(balance.receivedQuantity()).isEqualTo(5);
        assertThat(balance.issuedQuantity()).isEqualTo(3);
        assertThat(balance.stockQuantity()).isEqualTo(2);

        assertThatThrownBy(() -> inventory.createIssue(new GiftIssueRequest(unit.getId(), item.id(), recipient.getId(),
                LocalDate.now(), 3, "Sinh nhật", "PX-02", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vượt quá tồn kho");
        assertThatThrownBy(() -> inventory.deleteReceipt(receipt.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tồn kho");
    }

    @Test
    void restrictsRecipientSuggestionsToActiveMembersAndKeepsHistoricalSnapshotOnEdit() {
        UnionUnit unit = units.findByCodeIgnoreCase("VCS").orElseThrow();
        authenticateUser(unit.getId());
        var item = inventory.createItem(itemRequest(unit.getId(), "RECIPIENT"));
        inventory.createReceipt(new ReceiptRequest(unit.getId(), item.id(), LocalDate.now(), 2, null, null, null));
        Member active = member(unit, "REC-ACTIVE", MembershipStatus.MEMBER, EmploymentStatus.ACTIVE);
        Member inactive = member(unit, "REC-INACTIVE", MembershipStatus.MEMBER, EmploymentStatus.INACTIVE);

        var suggestions = inventory.searchRecipients(new ListQuery(null, null, true, "REC-", "employeeCode",
                unit.getId(), null, null));
        assertThat(suggestions).extracting(suggestion -> suggestion.memberId()).contains(active.getId());
        assertThat(suggestions).extracting(suggestion -> suggestion.memberId()).doesNotContain(inactive.getId());
        assertThatThrownBy(() -> inventory.createIssue(new GiftIssueRequest(unit.getId(), item.id(), inactive.getId(),
                LocalDate.now(), 1, "Sinh nhật", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đoàn viên đang làm việc");

        var issue = inventory.createIssue(new GiftIssueRequest(unit.getId(), item.id(), active.getId(), LocalDate.now(),
                1, "Sinh nhật", "PX-SNAPSHOT", null));
        String issuedName = issue.recipientName();
        active.setFullName("Tên hồ sơ đã đổi");
        active.setEmploymentStatus(EmploymentStatus.INACTIVE);
        members.save(active);

        var edited = inventory.updateIssue(issue.id(), new GiftIssueRequest(unit.getId(), item.id(), active.getId(),
                LocalDate.now().plusDays(1), 1, "Sinh nhật", "PX-SNAPSHOT", "Cập nhật ghi chú"));
        assertThat(edited.recipientName()).isEqualTo(issuedName);
        assertThat(edited.issueDate()).isEqualTo(LocalDate.now().plusDays(1));
    }

    private ItemRequest itemRequest(Long unitId, String suffix) {
        return new ItemRequest(unitId, "INV-" + suffix + "-" + token(), "Vật phẩm " + suffix,
                "Quà tặng", "Nhà cung cấp", "", 1, null);
    }

    private Member member(UnionUnit unit, String prefix, MembershipStatus membership, EmploymentStatus employment) {
        Member member = new Member();
        member.setEmployeeCode(prefix + "-" + token());
        member.setFullName("Đoàn viên " + prefix);
        member.setUnionUnit(unit);
        member.setMembershipStatus(membership);
        member.setEmploymentStatus(employment);
        member.setCompany(unit.getCompanyName());
        return members.save(member);
    }

    private String token() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private void authenticateUser(Long unitId) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("inventory-user-token")
                .header("alg", "none")
                .subject("user.inventory")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("unitId", unitId)
                .claim("roles", List.of("USER"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")), "user.inventory"));
    }
}
