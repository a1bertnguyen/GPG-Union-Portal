package vn.gpg.unionportal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ApiModels.WelfarePolicyRequest;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.model.DomainEnums.WelfarePolicySource;
import vn.gpg.unionportal.model.WelfarePolicy;
import vn.gpg.unionportal.repository.WelfarePolicyRepository;
import vn.gpg.unionportal.spec.Specs;
import vn.gpg.unionportal.spec.WelfarePolicySpecs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class WelfarePolicyService {
    private static final Sort SORT = Sort.by("sequenceNumber", "source", "id");

    private final WelfarePolicyRepository repository;
    private final RealtimeEventPublisher events;
    private final CurrentUserService currentUser;

    public WelfarePolicyService(WelfarePolicyRepository repository, RealtimeEventPublisher events,
                                CurrentUserService currentUser) {
        this.repository = repository;
        this.events = events;
        this.currentUser = currentUser;
    }

    public Page<WelfarePolicy> page(ListQuery query) {
        return repository.findAll(Specs.nullSafe(WelfarePolicySpecs.filter(query)), query.pageable(SORT));
    }

    public List<WelfarePolicy> search(ListQuery query) {
        return repository.findAll(Specs.nullSafe(WelfarePolicySpecs.filter(query)), SORT);
    }

    public ListFacets facets(ListQuery query) {
        List<WelfarePolicy> rows = search(query.withoutPaging());
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("total", rows.size());
        metrics.put("active", rows.stream().filter(WelfarePolicy::getActive).count());
        metrics.put("union", rows.stream().filter(item -> item.getSource() == WelfarePolicySource.UNION).count());
        metrics.put("company", rows.stream().filter(item -> item.getSource() == WelfarePolicySource.COMPANY).count());
        return new ListFacets(repository.count(), List.of("ACTIVE", "INACTIVE"), metrics);
    }

    @Transactional
    public WelfarePolicy create(WelfarePolicyRequest request) {
        requireAdmin();
        var saved = repository.save(apply(new WelfarePolicy(), request));
        events.changed("welfare-policies", "CREATED", saved.getId(), null);
        return saved;
    }

    @Transactional
    public WelfarePolicy update(Long id, WelfarePolicyRequest request) {
        requireAdmin();
        var saved = repository.save(apply(findById(id), request));
        events.changed("welfare-policies", "UPDATED", saved.getId(), null);
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        requireAdmin();
        var entity = findById(id);
        repository.delete(entity);
        events.changed("welfare-policies", "DELETED", entity.getId(), null);
    }

    public WelfarePolicy requireActive(Long id) {
        WelfarePolicy policy = require(id);
        if (!Boolean.TRUE.equals(policy.getActive())) {
            throw new IllegalArgumentException("Chính sách đã ngừng áp dụng, vui lòng chọn chính sách khác");
        }
        return policy;
    }

    public WelfarePolicy require(Long id) {
        return findById(id);
    }

    WelfarePolicy apply(WelfarePolicy entity, WelfarePolicyRequest request) {
        entity.setCode(request.code().trim());
        entity.setSource(request.source());
        entity.setSequenceNumber(request.sequenceNumber());
        entity.setWelfareType(request.welfareType());
        entity.setName(request.name().trim());
        entity.setSupportAmount(request.supportAmount());
        entity.setEligibilityNotes(trimToNull(request.eligibilityNotes()));
        entity.setProcessingWeeks(request.processingWeeks());
        entity.setActive(request.active());
        return entity;
    }

    private WelfarePolicy findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chính sách chăm lo với id=" + id));
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void requireAdmin() {
        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("Chỉ ADMIN được thay đổi chính sách chăm lo");
        }
    }
}
