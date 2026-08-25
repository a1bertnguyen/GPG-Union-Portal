package vn.gpg.unionportal.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.UnionUnitRequest;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.mapper.EntityMapper;
import vn.gpg.unionportal.model.UnionUnit;
import vn.gpg.unionportal.repository.UnionUnitRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UnionUnitService {
    private final UnionUnitRepository repository;
    private final EntityMapper mapper;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;

    public UnionUnitService(UnionUnitRepository repository, EntityMapper mapper, CurrentUserService currentUser,
                            RealtimeEventPublisher events) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.events = events;
    }

    public List<UnionUnit> list() {
        Long unitId = currentUser.scopedUnitId(null);
        if (unitId != null) {
            return List.of(repository.findById(unitId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CĐCS của tài khoản")));
        }
        return repository.findAll(Sort.by("code"));
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
