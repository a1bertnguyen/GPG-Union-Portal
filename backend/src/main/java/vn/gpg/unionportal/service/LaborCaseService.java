package vn.gpg.unionportal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.CaseGroupCount;
import vn.gpg.unionportal.dto.ApiModels.LaborCaseRequest;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.mapper.EntityMapper;
import vn.gpg.unionportal.model.DomainEnums.CaseSeverity;
import vn.gpg.unionportal.model.DomainEnums.CaseStatus;
import vn.gpg.unionportal.model.LaborCase;
import vn.gpg.unionportal.repository.LaborCaseRepository;
import vn.gpg.unionportal.spec.LaborCaseSpecs;
import vn.gpg.unionportal.spec.SpecAggregates;
import vn.gpg.unionportal.spec.Specs;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class LaborCaseService {
    private static final Sort SORT = Sort.by("deadline");
    /** "Ảnh hưởng nhiều NLĐ" threshold, kept in step with {@code LaborCaseSpecs}. */
    private static final int WIDE_IMPACT_THRESHOLD = 10;

    private final LaborCaseRepository repository;
    private final EntityMapper mapper;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;
    private final SpecAggregates aggregates;

    public LaborCaseService(LaborCaseRepository repository, EntityMapper mapper, CurrentUserService currentUser,
                            RealtimeEventPublisher events, SpecAggregates aggregates) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.events = events;
        this.aggregates = aggregates;
    }

    public Page<LaborCase> page(ListQuery query) {
        return repository.findAll(Specs.nullSafe(filter(query)), query.pageable(SORT));
    }

    public List<LaborCase> search(ListQuery query) {
        return repository.findAll(Specs.nullSafe(filter(query)), SORT);
    }

    public ListFacets facets(ListQuery query) {
        LocalDate today = LocalDate.now();
        Specification<LaborCase> scope = Specs.nullSafe(Specs.unitScope(scopedUnitId(query)));
        Specification<LaborCase> filtered = Specs.nullSafe(filter(query));
        Specification<LaborCase> open = filtered.and(Specs.notEq("status", CaseStatus.CLOSED));
        var counts = aggregates.countMetrics(LaborCase.class, filtered, Map.of(
                "open", open,
                "dueOrOverdue", open.and(Specs.onOrBefore("deadline", today)),
                "overdue", open.and(Specs.before("deadline", today)),
                "highSeverity", Specs.in("severity", List.of(CaseSeverity.HIGH, CaseSeverity.CRITICAL)),
                "closed", Specs.eq("status", CaseStatus.CLOSED),
                "wideImpact", Specs.atLeast("affectedPeople", WIDE_IMPACT_THRESHOLD),
                "repeated", LaborCaseSpecs.repeated(scopedUnitId(query))));
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("total", counts.total());
        metrics.put("open", counts.value("open"));
        metrics.put("dueOrOverdue", counts.value("dueOrOverdue"));
        metrics.put("overdue", counts.value("overdue"));
        metrics.put("highSeverity", counts.value("highSeverity"));
        metrics.put("closed", counts.value("closed"));
        metrics.put("wideImpact", counts.value("wideImpact"));
        metrics.put("repeated", counts.value("repeated"));
        metrics.put("affectedPeople", aggregates.sumLong(LaborCase.class, filtered, "affectedPeople"));
        return new ListFacets(
                repository.count(scope),
                aggregates.distinctValues(LaborCase.class, scope, "status"),
                metrics);
    }

    /** Per-issue-group rollup behind the case analytics bars. */
    public List<CaseGroupCount> issueGroups(ListQuery query) {
        return aggregates.caseGroups(Specs.nullSafe(filter(query)), LocalDate.now());
    }

    private Specification<LaborCase> filter(ListQuery query) {
        return LaborCaseSpecs.filter(query, scopedUnitId(query), LocalDate.now());
    }

    private Long scopedUnitId(ListQuery query) {
        return currentUser.scopedUnitId(query.unitId());
    }

    @Transactional
    public LaborCase create(LaborCaseRequest request) {
        currentUser.requireUnitAccess(request.unionUnitId());
        var saved = repository.save(mapper.apply(new LaborCase(), request));
        events.changed("cases", "CREATED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public LaborCase update(Long id, LaborCaseRequest request) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        currentUser.requireUnitAccess(request.unionUnitId());
        var saved = repository.save(mapper.apply(entity, request));
        events.changed("cases", "UPDATED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        repository.delete(entity);
        events.changed("cases", "DELETED", entity.getId(), entity.getUnionUnit().getId());
    }

    private LaborCase findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vụ việc với id=" + id));
    }
}
