package vn.gpg.unionportal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.CaseIssueGroupRequest;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.model.CaseIssueGroup;
import vn.gpg.unionportal.repository.CaseIssueGroupRepository;
import vn.gpg.unionportal.spec.Specs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class CaseIssueGroupService {
    private static final Sort SORT = Sort.by("name");

    private final CaseIssueGroupRepository repository;
    private final CurrentUserService currentUser;

    public CaseIssueGroupService(CaseIssueGroupRepository repository, CurrentUserService currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    public Page<CaseIssueGroup> page(ListQuery query) {
        return repository.findAll(filter(query), query.pageable(SORT));
    }

    public List<CaseIssueGroup> search(ListQuery query) {
        return repository.findAll(filter(query), SORT);
    }

    public ListFacets facets(ListQuery query) {
        var counts = repository.count(filter(query));
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("total", counts);
        metrics.put("active", repository.count(Specs.eq("active", true)));
        return new ListFacets(repository.count(), List.of("ACTIVE", "INACTIVE"), metrics);
    }

    @Transactional
    public CaseIssueGroup create(CaseIssueGroupRequest request) {
        requireAdmin();
        var entity = new CaseIssueGroup();
        return repository.save(apply(entity, request));
    }

    @Transactional
    public CaseIssueGroup update(Long id, CaseIssueGroupRequest request) {
        requireAdmin();
        return repository.save(apply(require(id), request));
    }

    @Transactional
    public void delete(Long id) {
        requireAdmin();
        repository.delete(require(id));
    }

    private Specification<CaseIssueGroup> filter(ListQuery query) {
        Specification<CaseIssueGroup> active = currentUser.isAdmin() ? null : Specs.eq("active", true);
        Specification<CaseIssueGroup> status = switch (query.statusValue() == null ? "" : query.statusValue()) {
            case "ACTIVE" -> Specs.eq("active", true);
            case "INACTIVE" -> Specs.eq("active", false);
            default -> null;
        };
        String raw = query.text();
        if (raw == null || raw.isBlank()) return Specs.nullSafe(Specs.allOf(active, status));
        String needle = "%" + raw.trim().toLowerCase(Locale.ROOT) + "%";
        Specification<CaseIssueGroup> matching = (root, criteria, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), needle), cb.like(cb.lower(root.get("name")), needle));
        return Specs.nullSafe(Specs.allOf(active, status, matching));
    }

    private CaseIssueGroup apply(CaseIssueGroup entity, CaseIssueGroupRequest request) {
        entity.setCode(request.code().trim());
        entity.setName(request.name().trim());
        entity.setActive(request.active());
        return entity;
    }

    private CaseIssueGroup require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhóm vấn đề với id=" + id));
    }

    private void requireAdmin() {
        if (!currentUser.isAdmin()) throw new AccessDeniedException("Chỉ ADMIN được quản lý nhóm vấn đề");
    }
}
