package vn.gpg.unionportal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ApiModels.UnionUnitRequest;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.mapper.EntityMapper;
import vn.gpg.unionportal.model.DomainEnums.LegalStatus;
import vn.gpg.unionportal.model.UnionUnit;
import vn.gpg.unionportal.repository.UnionUnitRepository;
import vn.gpg.unionportal.spec.SpecAggregates;
import vn.gpg.unionportal.spec.Specs;
import vn.gpg.unionportal.spec.UnionUnitSpecs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class UnionUnitService {
    private static final Sort SORT = Sort.by("code");

    private final UnionUnitRepository repository;
    private final EntityMapper mapper;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;
    private final SpecAggregates aggregates;

    public UnionUnitService(UnionUnitRepository repository, EntityMapper mapper, CurrentUserService currentUser,
                            RealtimeEventPublisher events, SpecAggregates aggregates) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.events = events;
        this.aggregates = aggregates;
    }

    public Page<UnionUnit> page(ListQuery query) {
        return repository.findAll(Specs.nullSafe(filter(query)), query.pageable(SORT));
    }

    /** Every CĐCS the caller may see. Feeds the unit dropdowns across the app. */
    public List<UnionUnit> search(ListQuery query) {
        return repository.findAll(Specs.nullSafe(filter(query)), SORT);
    }

    public ListFacets facets(ListQuery query) {
        Specification<UnionUnit> scope = Specs.nullSafe(ownScope());
        Specification<UnionUnit> filtered = Specs.nullSafe(filter(query));
        var counts = aggregates.countMetrics(UnionUnit.class, filtered, Map.of(
                "active", Specs.eq("legalStatus", LegalStatus.ACTIVE),
                "inactive", Specs.eq("legalStatus", LegalStatus.INACTIVE),
                "withChairperson", Specs.isPresent("chairperson")));
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("total", counts.total());
        metrics.put("active", counts.value("active"));
        metrics.put("inactive", counts.value("inactive"));
        metrics.put("withChairperson", counts.value("withChairperson"));
        return new ListFacets(
                repository.count(scope),
                aggregates.distinctValues(UnionUnit.class, scope, "legalStatus"),
                metrics);
    }

    /** A USER only ever sees their own CĐCS; an ADMIN sees them all. */
    private Specification<UnionUnit> ownScope() {
        Long unitId = currentUser.scopedUnitId(null);
        if (unitId == null) return null;
        return (root, criteria, cb) -> cb.equal(root.get("id"), unitId);
    }

    private Specification<UnionUnit> filter(ListQuery query) {
        return UnionUnitSpecs.filter(query, currentUser.scopedUnitId(null));
    }

    @Transactional
    public UnionUnit create(UnionUnitRequest request) {
        var saved = repository.save(mapper.apply(new UnionUnit(), request));
        events.changed("units", "CREATED", saved.getId(), saved.getId());
        return saved;
    }

    @Transactional
    public UnionUnit update(Long id, UnionUnitRequest request) {
        var entity = findById(id);
        var saved = repository.save(mapper.apply(entity, request));
        events.changed("units", "UPDATED", saved.getId(), saved.getId());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        var entity = findById(id);
        repository.delete(entity);
        events.changed("units", "DELETED", entity.getId(), entity.getId());
    }

    private UnionUnit findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CĐCS với id=" + id));
    }
}
