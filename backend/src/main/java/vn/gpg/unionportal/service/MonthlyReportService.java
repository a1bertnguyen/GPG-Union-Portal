package vn.gpg.unionportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.MonthlyReportRequest;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.mapper.EntityMapper;
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

    public MonthlyReportService(MonthlyReportRepository repository, EntityMapper mapper,
                                CurrentUserService currentUser) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUser = currentUser;
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
        currentUser.requireUnitAccess(request.unionUnitId());
        var reportMonth = YearMonth.parse(request.month()).atDay(1);
        var entity = repository.findByUnionUnitIdAndReportMonth(request.unionUnitId(), reportMonth)
                .orElseGet(MonthlyReport::new);
        return repository.save(mapper.apply(entity, request));
    }

    @Transactional
    public MonthlyReport update(Long id, MonthlyReportRequest request) {
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

    private MonthlyReport findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo với id=" + id));
    }
}
