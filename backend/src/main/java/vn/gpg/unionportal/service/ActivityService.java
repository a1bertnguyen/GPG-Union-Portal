package vn.gpg.unionportal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.ActivityRequest;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.mapper.EntityMapper;
import vn.gpg.unionportal.model.DomainEnums.ActivityStatus;
import vn.gpg.unionportal.model.UnionActivity;
import vn.gpg.unionportal.repository.UnionActivityRepository;
import vn.gpg.unionportal.spec.ActivitySpecs;
import vn.gpg.unionportal.spec.SpecAggregates;
import vn.gpg.unionportal.spec.Specs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ActivityService {
    private static final Sort SORT = Sort.by(Sort.Direction.DESC, "eventDate");

    private final UnionActivityRepository repository;
    private final EntityMapper mapper;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;
    private final SpecAggregates aggregates;

    public ActivityService(UnionActivityRepository repository, EntityMapper mapper, CurrentUserService currentUser,
                           RealtimeEventPublisher events, SpecAggregates aggregates) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.events = events;
        this.aggregates = aggregates;
    }

    public Page<UnionActivity> page(ListQuery query) {
        return repository.findAll(Specs.nullSafe(filter(query)), query.pageable(SORT));
    }

    public List<UnionActivity> search(ListQuery query) {
        return repository.findAll(Specs.nullSafe(filter(query)), SORT);
    }

    public ListFacets facets(ListQuery query) {
        Specification<UnionActivity> scope = Specs.nullSafe(Specs.unitScope(scopedUnitId(query)));
        Specification<UnionActivity> filtered = Specs.nullSafe(filter(query));
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("total", repository.count(filtered));
        metrics.put("planned", repository.count(filtered.and(Specs.eq("status", ActivityStatus.PLANNED))));
        metrics.put("inProgress", repository.count(filtered.and(Specs.eq("status", ActivityStatus.IN_PROGRESS))));
        metrics.put("completed", repository.count(filtered.and(Specs.eq("status", ActivityStatus.COMPLETED))));
        metrics.put("missingReport", repository.count(filtered
                .and(Specs.eq("status", ActivityStatus.COMPLETED))
                .and(Specs.isFalse("reportCompleted"))));
        return new ListFacets(
                repository.count(scope),
                aggregates.distinctValues(UnionActivity.class, scope, "status"),
                metrics);
    }

    private Specification<UnionActivity> filter(ListQuery query) {
        return ActivitySpecs.filter(query, scopedUnitId(query));
    }

    private Long scopedUnitId(ListQuery query) {
        return currentUser.scopedUnitId(query.unitId());
    }

    @Transactional
    public UnionActivity create(ActivityRequest request) {
        currentUser.requireUnitAccess(request.unionUnitId());
        var saved = repository.save(mapper.apply(new UnionActivity(), request));
        events.changed("activities", "CREATED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public UnionActivity update(Long id, ActivityRequest request) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        currentUser.requireUnitAccess(request.unionUnitId());
        var saved = repository.save(mapper.apply(entity, request));
        events.changed("activities", "UPDATED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        repository.delete(entity);
        events.changed("activities", "DELETED", entity.getId(), entity.getUnionUnit().getId());
    }

    private UnionActivity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hoạt động với id=" + id));
    }
}
