package vn.gpg.unionportal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.FinanceRequest;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.mapper.EntityMapper;
import vn.gpg.unionportal.model.FinanceEntry;
import vn.gpg.unionportal.repository.FinanceEntryRepository;
import vn.gpg.unionportal.spec.FinanceSpecs;
import vn.gpg.unionportal.spec.SpecAggregates;
import vn.gpg.unionportal.spec.Specs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class FinanceService {
    private static final Sort SORT = Sort.by(Sort.Direction.DESC, "transactionDate");

    private final FinanceEntryRepository repository;
    private final EntityMapper mapper;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;
    private final SpecAggregates aggregates;

    public FinanceService(FinanceEntryRepository repository, EntityMapper mapper, CurrentUserService currentUser,
                          RealtimeEventPublisher events, SpecAggregates aggregates) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.events = events;
        this.aggregates = aggregates;
    }

    public Page<FinanceEntry> page(ListQuery query) {
        return repository.findAll(Specs.nullSafe(filter(query)), query.pageable(SORT));
    }

    public List<FinanceEntry> search(ListQuery query) {
        return repository.findAll(Specs.nullSafe(filter(query)), SORT);
    }

    public ListFacets facets(ListQuery query) {
        Specification<FinanceEntry> scope = Specs.nullSafe(Specs.unitScope(scopedUnitId(query)));
        Specification<FinanceEntry> filtered = Specs.nullSafe(filter(query));
        var totals = aggregates.financeTotals(filtered);
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("total", repository.count(filtered));
        metrics.put("income", totals.income());
        metrics.put("expense", totals.expense());
        metrics.put("balance", totals.income().subtract(totals.expense()));
        metrics.put("incompleteDocuments", totals.incompleteDocuments());
        return new ListFacets(
                repository.count(scope),
                aggregates.distinctValues(FinanceEntry.class, scope, "entryType"),
                metrics);
    }

    private Specification<FinanceEntry> filter(ListQuery query) {
        return FinanceSpecs.filter(query, scopedUnitId(query));
    }

    private Long scopedUnitId(ListQuery query) {
        return currentUser.scopedUnitId(query.unitId());
    }

    @Transactional
    public FinanceEntry create(FinanceRequest request) {
        currentUser.requireUnitAccess(request.unionUnitId());
        var saved = repository.save(mapper.apply(new FinanceEntry(), request));
        events.changed("finance", "CREATED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public FinanceEntry update(Long id, FinanceRequest request) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        currentUser.requireUnitAccess(request.unionUnitId());
        var saved = repository.save(mapper.apply(entity, request));
        events.changed("finance", "UPDATED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        repository.delete(entity);
        events.changed("finance", "DELETED", entity.getId(), entity.getUnionUnit().getId());
    }

    private FinanceEntry findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch với id=" + id));
    }
}
