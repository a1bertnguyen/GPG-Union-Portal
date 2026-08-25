package vn.gpg.unionportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.LaborCaseRequest;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.mapper.EntityMapper;
import vn.gpg.unionportal.model.LaborCase;
import vn.gpg.unionportal.repository.LaborCaseRepository;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LaborCaseService {
    private final LaborCaseRepository repository;
    private final EntityMapper mapper;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;

    public LaborCaseService(LaborCaseRepository repository, EntityMapper mapper, CurrentUserService currentUser,
                            RealtimeEventPublisher events) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.events = events;
    }

    public List<LaborCase> list(Long unitId) {
        Long scopedUnitId = currentUser.scopedUnitId(unitId);
        return repository.findAll().stream()
                .filter(item -> scopedUnitId == null || item.getUnionUnit().getId().equals(scopedUnitId))
                .sorted(Comparator.comparing(LaborCase::getDeadline))
                .toList();
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
