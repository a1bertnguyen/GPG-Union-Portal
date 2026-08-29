package vn.gpg.unionportal.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.MonthlyReportRequest;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.mapper.EntityMapper;
import vn.gpg.unionportal.model.DomainEnums.ReportStatus;
import vn.gpg.unionportal.model.MonthlyReport;
import vn.gpg.unionportal.repository.MonthlyReportRepository;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MonthlyReportService {
    private final MonthlyReportRepository repository;
    private final EntityMapper mapper;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;

    public MonthlyReportService(MonthlyReportRepository repository, EntityMapper mapper,
                                CurrentUserService currentUser, RealtimeEventPublisher events) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.events = events;
    }

    public List<MonthlyReport> list() {
        Long unitId = currentUser.scopedUnitId(null);
        return repository.findAll().stream()
                .filter(item -> unitId == null || item.getUnionUnit().getId().equals(unitId))
                .sorted(Comparator.comparing(MonthlyReport::getReportMonth).reversed())
                .toList();
    }

    @Transactional
    public MonthlyReport upsert(MonthlyReportRequest request) {
        requireUserSubmission(request);
        currentUser.requireUnitAccess(request.unionUnitId());
        var reportMonth = YearMonth.parse(request.month()).atDay(1);
        var entity = repository.findByUnionUnitIdAndReportMonth(request.unionUnitId(), reportMonth)
                .orElseGet(MonthlyReport::new);
        requireEditableByUser(entity);
        boolean creating = entity.getId() == null;
        var saved = repository.save(mapper.apply(entity, request));
        events.changed("reports", creating ? "CREATED" : "UPDATED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public MonthlyReport update(Long id, MonthlyReportRequest request) {
        requireUserSubmission(request);
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        requireEditableByUser(entity);
        currentUser.requireUnitAccess(request.unionUnitId());
        var saved = repository.save(mapper.apply(entity, request));
        events.changed("reports", "UPDATED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public MonthlyReport approve(Long id) {
        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("Chỉ ADMIN được duyệt báo cáo tháng");
        }
        var entity = findById(id);
        if (entity.getStatus() != ReportStatus.SUBMITTED) {
            throw new IllegalArgumentException("Chỉ có thể duyệt báo cáo đã được USER nộp");
        }
        entity.setStatus(ReportStatus.APPROVED);
        var saved = repository.save(entity);
        events.changed("reports", "APPROVED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        if (!currentUser.isAdmin() && entity.getStatus() != ReportStatus.DRAFT) {
            throw new AccessDeniedException("Báo cáo đã nộp cho ADMIN không thể xóa");
        }
        repository.delete(entity);
        events.changed("reports", "DELETED", entity.getId(), entity.getUnionUnit().getId());
    }

    private MonthlyReport findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo với id=" + id));
    }

    private void requireUserSubmission(MonthlyReportRequest request) {
        if (currentUser.isAdmin()) {
            throw new AccessDeniedException("ADMIN chỉ theo dõi và duyệt báo cáo do USER nộp");
        }
        if (request.status() == ReportStatus.APPROVED) {
            throw new AccessDeniedException("USER không được tự duyệt báo cáo tháng");
        }
    }

    private void requireEditableByUser(MonthlyReport entity) {
        if (entity.getId() != null && entity.getStatus() != ReportStatus.DRAFT) {
            throw new AccessDeniedException("Báo cáo đã nộp cho ADMIN và không thể chỉnh sửa");
        }
    }
}
