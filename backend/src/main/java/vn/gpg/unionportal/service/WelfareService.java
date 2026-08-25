package vn.gpg.unionportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.WelfareRequest;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.mapper.EntityMapper;
import vn.gpg.unionportal.model.WelfareRecord;
import vn.gpg.unionportal.repository.WelfareRecordRepository;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class WelfareService {
    private final WelfareRecordRepository repository;
    private final EntityMapper mapper;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;

    public WelfareService(WelfareRecordRepository repository, EntityMapper mapper, CurrentUserService currentUser,
                          RealtimeEventPublisher events) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.events = events;
    }

    public List<WelfareRecord> list(Long unitId) {
        Long scopedUnitId = currentUser.scopedUnitId(unitId);
        return repository.findAll().stream()
                .filter(item -> scopedUnitId == null || item.getUnionUnit().getId().equals(scopedUnitId))
                .sorted(Comparator.comparing(WelfareRecord::getEventDate).reversed())
                .toList();
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
