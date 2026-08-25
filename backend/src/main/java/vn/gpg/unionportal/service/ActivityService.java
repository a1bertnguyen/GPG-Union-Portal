package vn.gpg.unionportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.ActivityRequest;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.mapper.EntityMapper;
import vn.gpg.unionportal.model.UnionActivity;
import vn.gpg.unionportal.repository.UnionActivityRepository;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ActivityService {
    private final UnionActivityRepository repository;
    private final EntityMapper mapper;
    private final CurrentUserService currentUser;

    public ActivityService(UnionActivityRepository repository, EntityMapper mapper, CurrentUserService currentUser) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    public List<UnionActivity> list(Long unitId) {
        Long scopedUnitId = currentUser.scopedUnitId(unitId);
        return repository.findAll().stream()
                .filter(item -> scopedUnitId == null || item.getUnionUnit().getId().equals(scopedUnitId))
                .sorted(Comparator.comparing(UnionActivity::getEventDate).reversed())
                .toList();
    }

    @Transactional
    public UnionActivity create(ActivityRequest request) {
        currentUser.requireUnitAccess(request.unionUnitId());
        return repository.save(mapper.apply(new UnionActivity(), request));
    }

    @Transactional
    public UnionActivity update(Long id, ActivityRequest request) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        currentUser.requireUnitAccess(request.unionUnitId());
        return repository.save(mapper.apply(entity, request));
    }

    @Transactional
    public void delete(Long id) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        repository.delete(entity);
    }

    private UnionActivity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hoạt động với id=" + id));
    }
}
