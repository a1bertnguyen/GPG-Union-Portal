package vn.gpg.unionportal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.dto.ApiModels.DocumentLibraryView;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.model.DocumentLibraryItem;
import vn.gpg.unionportal.model.UnionUnit;
import vn.gpg.unionportal.repository.DocumentLibraryRepository;
import vn.gpg.unionportal.repository.UnionUnitRepository;
import vn.gpg.unionportal.spec.Specs;

import java.io.IOException;

@Service
@Transactional(readOnly = true)
public class DocumentLibraryService {
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final Sort SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final DocumentLibraryRepository repository;
    private final UnionUnitRepository units;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;

    public DocumentLibraryService(DocumentLibraryRepository repository, UnionUnitRepository units,
                                  CurrentUserService currentUser, RealtimeEventPublisher events) {
        this.repository = repository;
        this.units = units;
        this.currentUser = currentUser;
        this.events = events;
    }

    public Page<DocumentLibraryView> page(ListQuery query) {
        return repository.findAll(filter(query), query.pageable(SORT)).map(this::view);
    }

    public java.util.List<DocumentLibraryView> search(ListQuery query) {
        return repository.findAll(filter(query), SORT).stream().map(this::view).toList();
    }

    @Transactional
    public DocumentLibraryView upload(Long unionUnitId, String category, String title,
                                      String description, MultipartFile file) {
        requireAdmin();
        validateText(category, "Nhóm tài liệu", 120);
        validateText(title, "Tên tài liệu", 200);
        validateFile(file);
        UnionUnit unit = units.findById(unionUnitId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CĐCS với id=" + unionUnitId));

        var item = new DocumentLibraryItem();
        item.setUnionUnit(unit);
        item.setCategory(category.trim());
        item.setTitle(title.trim());
        item.setDescription(trimToNull(description, 1000));
        item.setFileName(safeFileName(file.getOriginalFilename()));
        item.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        item.setFileSize(file.getSize());
        item.setFileData(bytes(file));
        item.setUploadedBy(currentUser.username());
        var saved = repository.save(item);
        events.changed("document-library", "CREATED", saved.getId(), unionUnitId);
        return view(saved);
    }

    public StoredFile download(Long id) {
        DocumentLibraryItem item = requireItem(id);
        return new StoredFile(item.getFileName(), item.getContentType(), item.getFileData());
    }

    @Transactional
    public void delete(Long id) {
        requireAdmin();
        DocumentLibraryItem item = requireItem(id);
        Long unitId = item.getUnionUnit().getId();
        repository.delete(item);
        events.changed("document-library", "DELETED", id, unitId);
    }

    private Specification<DocumentLibraryItem> filter(ListQuery query) {
        Long unitId = currentUser.scopedUnitId(query.unitId());
        Specification<DocumentLibraryItem> search = query.text().isEmpty() ? null : (root, criteria, cb) -> cb.or(
                Specs.textLike(cb, root.get("title"), query.text()),
                Specs.textLike(cb, root.get("category"), query.text()),
                Specs.textLike(cb, root.get("description"), query.text()),
                Specs.textLike(cb, root.get("fileName"), query.text()));
        return Specs.nullSafe(Specs.allOf(Specs.unitScope(unitId), search));
    }

    private DocumentLibraryItem requireItem(Long id) {
        DocumentLibraryItem item = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu với id=" + id));
        currentUser.requireUnitAccess(item.getUnionUnit().getId());
        return item;
    }

    private DocumentLibraryView view(DocumentLibraryItem item) {
        return new DocumentLibraryView(item.getId(), item.getUnionUnit(), item.getCategory(), item.getTitle(),
                item.getDescription(), item.getFileName(), item.getContentType(), item.getFileSize(),
                item.getUploadedBy(), item.getCreatedAt());
    }

    private void requireAdmin() {
        if (!currentUser.isAdmin()) throw new AccessDeniedException("Chỉ ADMIN được quản lý kho tài liệu");
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn tệp cần tải lên");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("Tệp không được lớn hơn 20 MB");
    }

    private void validateText(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " không được để trống");
        if (value.trim().length() > maxLength) throw new IllegalArgumentException(label + " quá dài");
    }

    private String trimToNull(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) throw new IllegalArgumentException("Mô tả quá dài");
        return trimmed;
    }

    private byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể đọc nội dung tệp", exception);
        }
    }

    private String safeFileName(String name) {
        if (name == null || name.isBlank()) return "tai-lieu";
        String safe = name.replace('\\', '_').replace('/', '_').trim();
        return safe.length() <= 255 ? safe : safe.substring(safe.length() - 255);
    }

    public record StoredFile(String fileName, String contentType, byte[] data) {
    }
}
