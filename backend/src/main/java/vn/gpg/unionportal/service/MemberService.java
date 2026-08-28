package vn.gpg.unionportal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ApiModels.MemberRequest;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.mapper.EntityMapper;
import vn.gpg.unionportal.model.DomainEnums.EmploymentStatus;
import vn.gpg.unionportal.model.DomainEnums.MembershipStatus;
import vn.gpg.unionportal.model.Member;
import vn.gpg.unionportal.repository.MemberRepository;
import vn.gpg.unionportal.repository.MemberChangeRepository;
import vn.gpg.unionportal.model.MemberChange;
import vn.gpg.unionportal.spec.MemberSpecs;
import vn.gpg.unionportal.spec.SpecAggregates;
import vn.gpg.unionportal.spec.Specs;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class MemberService {
    private static final Sort SORT = Sort.by("fullName");

    private final MemberRepository repository;
    private final EntityMapper mapper;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;
    private final MemberChangeRepository changes;
    private final SpecAggregates aggregates;

    public MemberService(MemberRepository repository, EntityMapper mapper, CurrentUserService currentUser,
                         RealtimeEventPublisher events, MemberChangeRepository changes, SpecAggregates aggregates) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.events = events;
        this.changes = changes;
        this.aggregates = aggregates;
    }

    /** One page of members matching the query, ordered by name. */
    public Page<Member> page(ListQuery query) {
        return repository.findAll(Specs.nullSafe(filter(query)), query.pageable(SORT));
    }

    /** Every member matching the query. Used by {@code all=true} lookups and the Excel export. */
    public List<Member> search(ListQuery query) {
        return repository.findAll(Specs.nullSafe(filter(query)), SORT);
    }

    public ListFacets facets(ListQuery query) {
        Specification<Member> scope = Specs.nullSafe(Specs.unitScope(scopedUnitId(query)));
        Specification<Member> filtered = Specs.nullSafe(filter(query));
        var counts = aggregates.countMetrics(Member.class, filtered, Map.of(
                "unionMembers", Specs.eq("membershipStatus", MembershipStatus.MEMBER),
                "notJoined", Specs.eq("membershipStatus", MembershipStatus.NOT_JOINED),
                "activeEmployment", Specs.eq("employmentStatus", EmploymentStatus.ACTIVE)));
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("total", counts.total());
        metrics.put("unionMembers", counts.value("unionMembers"));
        metrics.put("notJoined", counts.value("notJoined"));
        metrics.put("activeEmployment", counts.value("activeEmployment"));
        return new ListFacets(
                repository.count(scope),
                aggregates.distinctValues(Member.class, scope, "membershipStatus"),
                metrics);
    }

    private Specification<Member> filter(ListQuery query) {
        return MemberSpecs.filter(query, scopedUnitId(query));
    }

    private Long scopedUnitId(ListQuery query) {
        return currentUser.scopedUnitId(query.unitId());
    }

    @Transactional
    public Member create(MemberRequest request) {
        currentUser.requireUnitAccess(request.unionUnitId());
        var saved = repository.save(mapper.apply(new Member(), request));
        recordChange(saved, "THÊM ĐOÀN VIÊN", "Khởi tạo hồ sơ đoàn viên trên hệ thống.");
        events.changed("members", "CREATED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public Member update(Long id, MemberRequest request) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        currentUser.requireUnitAccess(request.unionUnitId());
        String before = snapshot(entity);
        var saved = repository.save(mapper.apply(entity, request));
        String after = snapshot(saved);
        if (!Objects.equals(before, after)) {
            recordChange(saved, "CẬP NHẬT HỒ SƠ", "Thông tin hồ sơ được cập nhật: " + after);
        }
        events.changed("members", "UPDATED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        repository.delete(entity);
        events.changed("members", "DELETED", entity.getId(), entity.getUnionUnit().getId());
    }

    private Member findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đoàn viên với id=" + id));
    }

    private String snapshot(Member member) {
        return "CĐCS " + member.getUnionUnit().getCode()
                + "; trạng thái công đoàn " + member.getMembershipStatus()
                + "; trạng thái nhân sự " + member.getEmploymentStatus()
                + "; chức danh " + Objects.toString(member.getJobTitle(), "chưa cập nhật");
    }

    private void recordChange(Member member, String type, String description) {
        var change = new MemberChange();
        change.setMember(member);
        change.setChangeType(type);
        change.setEffectiveDate(LocalDate.now());
        change.setDescription(description);
        change.setRecordedBy(currentUser.username());
        changes.save(change);
    }
}
