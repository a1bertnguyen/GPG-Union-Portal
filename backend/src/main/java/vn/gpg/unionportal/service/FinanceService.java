package vn.gpg.unionportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.FinanceRequest;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.mapper.EntityMapper;
import vn.gpg.unionportal.model.FinanceEntry;
import vn.gpg.unionportal.repository.FinanceEntryRepository;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class FinanceService {
    private final FinanceEntryRepository repository;
    private final EntityMapper mapper;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;

    public FinanceService(FinanceEntryRepository repository, EntityMapper mapper, CurrentUserService currentUser,
                          RealtimeEventPublisher events) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.events = events;
    }

    public List<FinanceEntry> list(Long unitId) {
        Long scopedUnitId = currentUser.scopedUnitId(unitId);
        return repository.findAll().stream()
                .filter(item -> scopedUnitId == null || item.getUnionUnit().getId().equals(scopedUnitId))
                .sorted(Comparator.comparing(FinanceEntry::getTransactionDate).reversed())
                .toList();
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
