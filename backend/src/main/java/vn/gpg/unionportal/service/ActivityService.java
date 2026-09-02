package vn.gpg.unionportal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.ActivityRequest;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.mapper.EntityMapper;
import vn.gpg.unionportal.model.DomainEnums.ActivityMediaType;
import vn.gpg.unionportal.model.DomainEnums.ActivityStatus;
import vn.gpg.unionportal.model.DomainEnums.DocumentStatus;
import vn.gpg.unionportal.model.UnionActivity;
import vn.gpg.unionportal.repository.ActivityMediaRepository;
import vn.gpg.unionportal.repository.UnionActivityRepository;
import vn.gpg.unionportal.spec.ActivitySpecs;
import vn.gpg.unionportal.spec.SpecAggregates;
import vn.gpg.unionportal.spec.Specs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ActivityService {
    private static final Sort SORT = Sort.by(Sort.Direction.DESC, "eventDate");

    private final UnionActivityRepository repository;
    private final ActivityMediaRepository media;
    private final EntityMapper mapper;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;
    private final SpecAggregates aggregates;

    public ActivityService(UnionActivityRepository repository, ActivityMediaRepository media,
                           EntityMapper mapper, CurrentUserService currentUser,
                           RealtimeEventPublisher events, SpecAggregates aggregates) {
        this.repository = repository;
        this.media = media;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.events = events;
        this.aggregates = aggregates;
    }

    public Page<UnionActivity> page(ListQuery query) {
        return repository.findAll(Specs.nullSafe(filter(query)), query.pageable(SORT));
    }

    public List<UnionActivity> search(ListQuery query) {
        return repository.findAll(Specs.nullSafe(filter(query)), SORT);
    }

    public ListFacets facets(ListQuery query) {
        Specification<UnionActivity> scope = Specs.nullSafe(Specs.unitScope(scopedUnitId(query)));
        Specification<UnionActivity> filtered = Specs.nullSafe(filter(query));
        var counts = aggregates.countMetrics(UnionActivity.class, filtered, Map.of(
                "planned", Specs.eq("status", ActivityStatus.PLANNED),
                "inProgress", Specs.eq("status", ActivityStatus.IN_PROGRESS),
                "completed", Specs.eq("status", ActivityStatus.COMPLETED),
                "missingReport", Specs.<UnionActivity>eq("status", ActivityStatus.COMPLETED)
                        .and(Specs.isFalse("reportCompleted"))));
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("total", counts.total());
        metrics.put("planned", counts.value("planned"));
        metrics.put("inProgress", counts.value("inProgress"));
        metrics.put("completed", counts.value("completed"));
        metrics.put("missingReport", counts.value("missingReport"));
        return new ListFacets(
                repository.count(scope),
                aggregates.distinctValues(UnionActivity.class, scope, "status"),
                metrics);
    }

    private Specification<UnionActivity> filter(ListQuery query) {
        return ActivitySpecs.filter(query, scopedUnitId(query));
    }

    private Long scopedUnitId(ListQuery query) {
        return currentUser.scopedUnitId(query.unitId());
    }

    @Transactional
    public UnionActivity create(ActivityRequest request) {
        currentUser.requireUnitAccess(request.unionUnitId());
        validateWorkflow(null, request);
        var saved = repository.save(mapper.apply(new UnionActivity(), request));
        events.changed("activities", "CREATED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public UnionActivity update(Long id, ActivityRequest request) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        currentUser.requireUnitAccess(request.unionUnitId());
        validateWorkflow(id, request);
        var saved = repository.save(mapper.apply(entity, request));
        events.changed("activities", "UPDATED", saved.getId(), saved.getUnionUnit().getId());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        var entity = findById(id);
        currentUser.requireUnitAccess(entity.getUnionUnit().getId());
        repository.delete(entity);
        events.changed("activities", "DELETED", entity.getId(), entity.getUnionUnit().getId());
    }

    private UnionActivity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hoạt động với id=" + id));
    }

    private void validateWorkflow(Long activityId, ActivityRequest request) {
        boolean submitted = Boolean.TRUE.equals(request.reportCompleted());
        boolean closing = request.status() == ActivityStatus.COMPLETED;
        if (closing && !submitted) {
            throw new IllegalArgumentException("Chỉ được đóng chương trình sau khi đã nộp báo cáo");
        }
        if (!submitted) return;

        List<String> missing = new ArrayList<>();
        required(missing, request.eventTime(), "giờ tổ chức");
        required(missing, request.location(), "địa điểm");
        required(missing, request.programPic(), "PIC chương trình");
        required(missing, request.objective(), "mục tiêu");
        if (request.invitedCount() == null || request.invitedCount() <= 0) missing.add("số người mời");
        required(missing, request.employeeGroup(), "nhóm NLĐ");
        required(missing, request.actualContent(), "nội dung thực tế");
        required(missing, request.planDifference(), "khác biệt so với kế hoạch");
        required(missing, request.quickFeedback(), "báo cáo");
        required(missing, request.issues(), "vấn đề ghi nhận");
        required(missing, request.outputProposal(), "đề xuất");
        required(missing, request.communicationContent(), "nội dung truyền thông");
        required(missing, request.participantList(), "danh sách tham dự");
        if (request.usefulnessScore() == null) missing.add("điểm hữu ích");
        required(missing, request.strengths(), "điều làm tốt");
        required(missing, request.weaknesses(), "điều chưa tốt");
        required(missing, request.lessonsLearned(), "bài học");
        required(missing, request.followUpIssue(), "vấn đề cần follow-up");
        required(missing, request.followUpStatus(), "tình trạng follow-up");
        if (request.documentStatus() != DocumentStatus.COMPLETE) missing.add("tình trạng chứng từ đầy đủ");
        if (activityId == null || !media.existsByActivityIdAndMediaType(activityId, ActivityMediaType.PHOTO)) {
            missing.add("ít nhất 1 ảnh");
        }
        if (activityId == null || !media.existsByActivityIdAndMediaType(activityId, ActivityMediaType.DOCUMENT)) {
            missing.add("ít nhất 1 chứng từ");
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Báo cáo chương trình còn thiếu: " + String.join(", ", missing));
        }

        if (closing && (isBlank(request.followUpOwner()) || request.followUpDeadline() == null)) {
            throw new IllegalArgumentException("Chỉ được đóng chương trình khi follow-up đã có PIC và deadline");
        }
    }

    private void required(List<String> missing, Object value, String label) {
        if (value == null || value instanceof String text && text.isBlank()) missing.add(label);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
