package vn.gpg.unionportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.MemberRequest;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.mapper.EntityMapper;
import vn.gpg.unionportal.model.Member;
import vn.gpg.unionportal.repository.MemberRepository;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository repository;
    private final EntityMapper mapper;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;

    public MemberService(MemberRepository repository, EntityMapper mapper, CurrentUserService currentUser,
                         RealtimeEventPublisher events) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.events = events;
    }

    public List<Member> list(Long unitId, String searchText) {
        Long scopedUnitId = currentUser.scopedUnitId(unitId);
        String query = searchText == null ? "" : searchText.trim().toLowerCase();
        return repository.findAll().stream()
                .filter(member -> scopedUnitId == null || member.getUnionUnit().getId().equals(scopedUnitId))
                .filter(member -> query.isEmpty() || member.getFullName().toLowerCase().contains(query)
                        || member.getEmployeeCode().toLowerCase().contains(query))
                .sorted(Comparator.comparing(Member::getFullName))
                .toList();
    }

    @Transactional
    public Member create(MemberRequest request) {
        currentUser.requireUnitAccess(request.unionUnitId());
        var saved = repository.save(mapper.apply(new Member(), request));
        events.changed("members", "CREATED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public Member update(Long id, MemberRequest request) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        currentUser.requireUnitAccess(request.unionUnitId());
        var saved = repository.save(mapper.apply(entity, request));
        events.changed("members", "UPDATED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        repository.delete(entity);
        events.changed("members", "DELETED", entity.getId(), entity.getUnionUnit().getId());
    }

    private Member findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đoàn viên với id=" + id));
    }
}
