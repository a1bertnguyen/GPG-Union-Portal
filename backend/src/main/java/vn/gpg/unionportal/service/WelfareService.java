package vn.gpg.unionportal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ApiModels.WelfareRequest;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.mapper.EntityMapper;
import vn.gpg.unionportal.model.DomainEnums.WelfareType;
import vn.gpg.unionportal.model.DomainEnums.WorkStatus;
import vn.gpg.unionportal.model.WelfareRecord;
import vn.gpg.unionportal.repository.WelfareRecordRepository;
import vn.gpg.unionportal.spec.SpecAggregates;
import vn.gpg.unionportal.spec.Specs;
import vn.gpg.unionportal.spec.WelfareSpecs;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class WelfareService {
    private static final Sort SORT = Sort.by(Sort.Direction.DESC, "eventDate");

    private final WelfareRecordRepository repository;
    private final EntityMapper mapper;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;
    private final SpecAggregates aggregates;

    public WelfareService(WelfareRecordRepository repository, EntityMapper mapper, CurrentUserService currentUser,
                          RealtimeEventPublisher events, SpecAggregates aggregates) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.events = events;
        this.aggregates = aggregates;
    }

    public Page<WelfareRecord> page(ListQuery query) {
        return repository.findAll(Specs.nullSafe(filter(query)), query.pageable(SORT));
    }

    public List<WelfareRecord> search(ListQuery query) {
        return repository.findAll(Specs.nullSafe(filter(query)), SORT);
    }

    public ListFacets facets(ListQuery query) {
        LocalDate today = LocalDate.now();
        Specification<WelfareRecord> scope = Specs.nullSafe(Specs.unitScope(scopedUnitId(query)));
        Specification<WelfareRecord> filtered = Specs.nullSafe(filter(query));
        var counts = aggregates.countMetrics(WelfareRecord.class, filtered, Map.of(
                "birthday", Specs.eq("welfareType", WelfareType.BIRTHDAY),
                "visit", Specs.eq("welfareType", WelfareType.VISIT),
                "funeralOrWedding", Specs.in("welfareType", List.of(WelfareType.FUNERAL, WelfareType.WEDDING)),
                "unfinished", Specs.notIn("status", List.of(WorkStatus.COMPLETED, WorkStatus.CANCELLED)),
                "newRequests", Specs.eq("status", WorkStatus.NEW),
                "due", dueSoon(today)));
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("total", counts.total());
        metrics.put("birthday", counts.value("birthday"));
        metrics.put("visit", counts.value("visit"));
        metrics.put("funeralOrWedding", counts.value("funeralOrWedding"));
        metrics.put("unfinished", counts.value("unfinished"));
        metrics.put("newRequests", counts.value("newRequests"));
        metrics.put("due", counts.value("due"));
        return new ListFacets(
                repository.count(scope),
                aggregates.distinctValues(WelfareRecord.class, scope, "status"),
                metrics);
    }

    /** Not finished and due today or tomorrow — same rule as the "Đến hạn" tracking filter. */
    private Specification<WelfareRecord> dueSoon(LocalDate today) {
        return (root, criteria, cb) -> cb.and(
                cb.notEqual(root.get("status"), WorkStatus.COMPLETED),
                cb.lessThanOrEqualTo(cb.coalesce(root.get("deadline"), root.get("eventDate")), today.plusDays(1)));
    }

    private Specification<WelfareRecord> filter(ListQuery query) {
        return WelfareSpecs.filter(query, scopedUnitId(query), LocalDate.now());
    }

    private Long scopedUnitId(ListQuery query) {
        return currentUser.scopedUnitId(query.unitId());
    }

    @Transactional
    public WelfareRecord create(WelfareRequest request) {
        currentUser.requireUnitAccess(request.unionUnitId());
        var saved = repository.save(mapper.apply(new WelfareRecord(), request));
        events.changed("welfare", "CREATED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public WelfareRecord update(Long id, WelfareRequest request) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        currentUser.requireUnitAccess(request.unionUnitId());
        var saved = repository.save(mapper.apply(entity, request));
        events.changed("welfare", "UPDATED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        repository.delete(entity);
        events.changed("welfare", "DELETED", entity.getId(), entity.getUnionUnit().getId());
    }

    private WelfareRecord findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ chăm lo với id=" + id));
    }
}
