package vn.gpg.unionportal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.dto.ApiModels.ActivityMediaView;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.model.*;
import vn.gpg.unionportal.model.DomainEnums.ActivityMediaType;
import vn.gpg.unionportal.repository.ActivityMediaRepository;
import vn.gpg.unionportal.repository.UnionActivityRepository;
import vn.gpg.unionportal.spec.SpecAggregates;
import vn.gpg.unionportal.spec.Specs;
import vn.gpg.unionportal.spec.WorkspaceSpecs;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ActivityMediaService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Sort SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final ActivityMediaRepository media;
    private final UnionActivityRepository activities;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;
    private final SpecAggregates aggregates;

    public ActivityMediaService(ActivityMediaRepository media, UnionActivityRepository activities,
                                CurrentUserService currentUser, RealtimeEventPublisher events,
                                SpecAggregates aggregates) {
        this.media = media;
        this.activities = activities;
        this.currentUser = currentUser;
        this.events = events;
        this.aggregates = aggregates;
    }

    public List<ActivityMediaView> list(Long activityId) {
        return search(ListQuery.firstPage(), activityId, null);
    }

    public Page<ActivityMediaView> page(ListQuery query, Long activityId, String activityStatus) {
        return media.findAll(filter(query, activityId, activityStatus), query.pageable(SORT)).map(this::view);
    }

    public List<ActivityMediaView> search(ListQuery query, Long activityId, String activityStatus) {
        return media.findAll(filter(query, activityId, activityStatus), SORT).stream().map(this::view).toList();
    }

    /**
     * Counts per media type over the filtered set, so the gallery's "Ảnh / Tài liệu" tabs show real
     * totals rather than however many happen to be on the current page. The media-type filter is
     * dropped here on purpose — both tab counts have to be visible while one tab is selected.
     */
    public ListFacets facets(ListQuery query, Long activityId, String activityStatus) {
        Specification<ActivityMedia> scope = Specs.nullSafe(Specs.unitScopeVia("activity", scopedUnitId()));
        ListQuery anyType = new ListQuery(null, null, true, query.q(), query.searchField(), query.unitId(), null, null);
        Specification<ActivityMedia> withoutType = filter(anyType, activityId, activityStatus);
        var counts = aggregates.countMetrics(ActivityMedia.class, withoutType, Map.of(
                "photos", Specs.eq("mediaType", ActivityMediaType.PHOTO),
                "documents", Specs.eq("mediaType", ActivityMediaType.DOCUMENT)));
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("total", counts.total());
        metrics.put("photos", counts.value("photos"));
        metrics.put("documents", counts.value("documents"));
        return new ListFacets(media.count(scope), List.of("PHOTO", "DOCUMENT"), metrics);
    }

    private Specification<ActivityMedia> filter(ListQuery query, Long activityId, String activityStatus) {
        return Specs.nullSafe(WorkspaceSpecs.activityMedia(query, scopedUnitId(), activityId, activityStatus));
    }

    private Long scopedUnitId() {
        return currentUser.scopedUnitId(null);
    }

    @Transactional
    public ActivityMediaView upload(Long activityId, ActivityMediaType mediaType, String title, MultipartFile file) {
        UnionActivity activity = activities.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hoạt động với id=" + activityId));
        currentUser.requireUnitAccess(activity.getUnionUnit().getId());
        validateFile(file, mediaType);
        var item = new ActivityMedia();
        item.setActivity(activity);
        item.setMediaType(mediaType);
        item.setTitle(title == null || title.isBlank() ? null : title.trim());
        item.setFileName(safeFileName(file.getOriginalFilename()));
        item.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        item.setFileSize(file.getSize());
        item.setFileData(bytes(file));
        item.setUploadedBy(currentUser.username());
        var saved = media.save(item);
        events.changed("activity-media", "CREATED", saved.getId(), activity.getUnionUnit().getId());
        return view(saved);
    }

    public StoredFile download(Long id) {
        ActivityMedia item = requireItem(id);
        return new StoredFile(item.getFileName(), item.getContentType(), item.getFileData());
    }

    @Transactional
    public void delete(Long id) {
        ActivityMedia item = requireItem(id);
        Long unitId = item.getActivity().getUnionUnit().getId();
        media.delete(item);
        events.changed("activity-media", "DELETED", id, unitId);
    }

    private ActivityMedia requireItem(Long id) {
        ActivityMedia item = media.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tệp hoạt động với id=" + id));
        currentUser.requireUnitAccess(item.getActivity().getUnionUnit().getId());
        return item;
    }

    private ActivityMediaView view(ActivityMedia item) {
        UnionActivity activity = item.getActivity();
        return new ActivityMediaView(item.getId(), activity.getId(), activity.getActivityCode(), activity.getName(),
                activity.getUnionUnit(), item.getMediaType(), item.getTitle(), item.getFileName(), item.getContentType(),
                item.getFileSize(), item.getUploadedBy(), item.getCreatedAt());
    }

    private void validateFile(MultipartFile file, ActivityMediaType type) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn tệp cần tải lên");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("Tệp không được lớn hơn 10 MB");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (type == ActivityMediaType.PHOTO && !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Thư viện ảnh chỉ nhận tệp hình ảnh");
        }
    }

    private byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể đọc nội dung tệp", exception);
        }
    }

    private String safeFileName(String name) {
        if (name == null || name.isBlank()) return "tep-hoat-dong";
        String safe = name.replace('\\', '_').replace('/', '_').trim();
        return safe.length() <= 255 ? safe : safe.substring(safe.length() - 255);
    }

    public record StoredFile(String fileName, String contentType, byte[] data) {
    }
}
