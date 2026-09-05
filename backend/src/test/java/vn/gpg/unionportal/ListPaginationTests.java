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
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.Member;
import vn.gpg.unionportal.repository.MemberRepository;
import vn.gpg.unionportal.repository.UnionUnitRepository;
import vn.gpg.unionportal.service.LaborCaseService;
import vn.gpg.unionportal.service.MemberService;
import vn.gpg.unionportal.spec.MemberSpecs;
import vn.gpg.unionportal.service.WelfareService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the server-side list contract that replaced the old "load everything, filter in the
 * browser" approach: paging arithmetic, the {@code all=true} escape hatch, CĐCS scoping, the
 * tracking presets, Vietnamese label search, and facets that must agree with the paged totals.
 */
@SpringBootTest
@Transactional
class ListPaginationTests {
    @Autowired private MemberService members;
    @Autowired private WelfareService welfare;
    @Autowired private LaborCaseService cases;
    @Autowired private UnionUnitRepository units;
    @Autowired private MemberRepository memberRepository;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private static ListQuery query(Integer page, Integer size) {
        return new ListQuery(page, size, null, null, null, null, null, null);
    }

    private static ListQuery search(String text, String field) {
        return new ListQuery(null, null, null, text, field, null, null, null);
    }

    private static ListQuery preset(String preset) {
        return new ListQuery(null, null, null, null, null, null, null, preset);
    }

    @Test
    void pagesSplitTheResultSetWithoutLosingOrDuplicatingRows() {
        long total = members.page(query(null, null)).getTotalElements();
        assertThat(total).as("seed data should provide several members").isGreaterThan(2);

        var firstPage = members.page(query(0, 2));
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(total);
        assertThat(firstPage.getTotalPages()).isEqualTo((int) Math.ceil(total / 2d));

        List<Long> seen = new ArrayList<>();
        for (int page = 0; page < firstPage.getTotalPages(); page++) {
            members.page(query(page, 2)).getContent().stream().map(Member::getId).forEach(seen::add);
        }
        assertThat(seen).hasSize((int) total).doesNotHaveDuplicates();
    }

    @Test
    void defaultPageSizeAppliesWhenTheClientSendsNothing() {
        assertThat(members.page(ListQuery.firstPage()).getSize()).isEqualTo(ListQuery.DEFAULT_SIZE);
    }

    @Test
    void searchIsUnpagedSoLookupsAndExportsSeeEveryMatch() {
        long total = members.page(query(null, null)).getTotalElements();
        assertThat(members.search(query(0, 1).withoutPaging())).hasSize((int) total);
    }

    @Test
    void searchMatchesTheVietnameseEnumLabelNotJustTheConstant() {
        var byLabel = members.search(search("Chưa gia nhập", "membershipStatus"));
        var byConstant = members.search(search("NOT_JOINED", "membershipStatus"));

        assertThat(byLabel).isNotEmpty();
        assertThat(byLabel).extracting(Member::getId)
                .containsExactlyInAnyOrderElementsOf(byConstant.stream().map(Member::getId).toList());
    }

    @Test
    void searchAcrossAllFieldsStillFindsRowsByUnitCode() {
        assertThat(members.search(search("VCS", "all"))).isNotEmpty()
                .allMatch(member -> member.getUnionUnit().getCode().equals("VCS"));
    }

    @Test
    void missingPresetReturnsExactlyTheMembersWithAnIncompleteProfile() {
        var missing = members.search(preset("missing"));

        assertThat(missing).isNotEmpty();
        // Asserted through the production contract on purpose. The hand-copied field list this test used
        // before silently drifted the moment MemberSpecs.REQUIRED_FIELDS gained company and startWorkDate,
        // and the test then failed for a member whose profile really was incomplete.
        assertThat(missing).noneMatch(MemberSpecs::hasRequiredProfileFields);

        // The demo seed leaves every member short of at least one required field, so the complement of the
        // preset is empty and asserting over it proves nothing. Complete one profile to exercise the boundary
        // in both directions: a fully filled member must drop out of the preset.
        var candidate = missing.getFirst();
        candidate.setCompany("Công ty kiểm thử");
        candidate.setJobTitle("Nhân viên kiểm thử");
        candidate.setWorkplace("Xưởng kiểm thử");
        candidate.setPhone("0900000001");
        candidate.setEmail("kiem.thu@example.com");
        candidate.setJoinDate(LocalDate.of(2026, 1, 2));
        candidate.setStartWorkDate(LocalDate.of(2025, 1, 2));
        memberRepository.saveAndFlush(candidate);
        assertThat(MemberSpecs.hasRequiredProfileFields(candidate)).isTrue();

        var stillMissing = members.search(preset("missing"));
        assertThat(stillMissing).doesNotContain(candidate);
        assertThat(stillMissing).noneMatch(MemberSpecs::hasRequiredProfileFields);
        assertThat(members.search(ListQuery.firstPage())).contains(candidate);
    }

    @Test
    void repeatedCasePresetKeepsOnlyIssueGroupsSeenMoreThanOnce() {
        var repeated = cases.search(preset("repeated"));
        var all = cases.search(ListQuery.firstPage());

        assertThat(repeated).allMatch(item ->
                all.stream().filter(other -> other.getIssueGroup().equals(item.getIssueGroup())).count() > 1);
    }

    @Test
    void facetsCountTheWholeFilteredSetNotTheCurrentPage() {
        var facets = members.facets(query(0, 1));

        assertThat(members.page(query(0, 1)).getContent()).hasSize(1);
        assertThat(facets.metrics().get("total").longValue())
                .isEqualTo(members.page(query(0, 1)).getTotalElements());
        assertThat(facets.total()).isEqualTo(facets.metrics().get("total").longValue());
        assertThat(facets.statusValues()).isNotEmpty();
    }

    @Test
    void facetMetricsAddUpToTheMembershipBreakdown() {
        var facets = members.facets(ListQuery.firstPage());
        long member = facets.metrics().get("unionMembers").longValue();
        long notJoined = facets.metrics().get("notJoined").longValue();

        assertThat(member + notJoined).isLessThanOrEqualTo(facets.metrics().get("total").longValue());
    }

    @Test
    void welfareFacetsExposeTheTrackingCountsTheInsightScreenShows() {
        var facets = welfare.facets(ListQuery.firstPage());

        assertThat(facets.metrics()).containsKeys("total", "birthday", "visit", "funeralOrWedding", "unfinished", "due");
        assertThat(facets.metrics().get("unfinished").longValue())
                .isLessThanOrEqualTo(facets.metrics().get("total").longValue());
    }

    @Test
    void issueGroupRollupCoversEveryCaseInTheFilteredSet() {
        long grouped = cases.issueGroups(ListQuery.firstPage()).stream().mapToLong(group -> group.count()).sum();

        assertThat(grouped).isEqualTo(cases.page(ListQuery.firstPage()).getTotalElements());
    }

    @Test
    void userAccountsOnlySeeTheirOwnUnitEvenWhenAskingForAnother() {
        Long ownUnitId = units.findByCodeIgnoreCase("VCS").orElseThrow().getId();
        Long otherUnitId = units.findByCodeIgnoreCase("GPL").orElseThrow().getId();
        authenticateUserForUnit(ownUnitId);

        var requestingAnotherUnit = new ListQuery(null, null, true, null, null, otherUnitId, null, null);

        assertThat(members.search(requestingAnotherUnit))
                .isNotEmpty()
                .allMatch(member -> member.getUnionUnit().getId().equals(ownUnitId));
        assertThat(members.facets(requestingAnotherUnit).total())
                .isEqualTo(members.search(ListQuery.allForUnit(null)).size());
    }

    private void authenticateUserForUnit(Long unitId) {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("list-pagination-test-token")
                .header("alg", "HS256")
                .subject("user.vcs")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("roles", List.of("USER"))
                .claim("unitId", unitId)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")), "user.vcs"));
    }
}
