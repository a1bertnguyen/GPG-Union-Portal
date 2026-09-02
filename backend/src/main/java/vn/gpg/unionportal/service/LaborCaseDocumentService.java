package vn.gpg.unionportal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.dto.ApiModels.LaborCaseDocumentView;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.model.LaborCase;
import vn.gpg.unionportal.model.LaborCaseDocument;
import vn.gpg.unionportal.repository.LaborCaseDocumentRepository;
import vn.gpg.unionportal.repository.LaborCaseRepository;
import vn.gpg.unionportal.spec.Specs;

import java.io.IOException;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LaborCaseDocumentService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Sort SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final LaborCaseDocumentRepository documents;
    private final LaborCaseRepository cases;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;

    public LaborCaseDocumentService(LaborCaseDocumentRepository documents, LaborCaseRepository cases,
                                    CurrentUserService currentUser, RealtimeEventPublisher events) {
        this.documents = documents;
        this.cases = cases;
        this.currentUser = currentUser;
        this.events = events;
    }

    public Page<LaborCaseDocumentView> page(ListQuery query, Long caseId) {
        return documents.findAll(filter(caseId), query.pageable(SORT)).map(this::view);
    }

    public List<LaborCaseDocumentView> search(Long caseId) {
        return documents.findAll(filter(caseId), SORT).stream().map(this::view).toList();
    }

    @Transactional
    public LaborCaseDocumentView upload(Long caseId, MultipartFile file) {
        LaborCase laborCase = requireCase(caseId);
        validateFile(file);
        var document = new LaborCaseDocument();
        document.setLaborCase(laborCase);
        document.setFileName(safeFileName(file.getOriginalFilename()));
        document.setContentType(contentType(file));
        document.setFileSize(file.getSize());
        document.setFileData(bytes(file));
        document.setUploadedBy(currentUser.username());
        var saved = documents.save(document);
        events.changed("case-documents", "CREATED", saved.getId(), laborCase.getUnionUnit().getId());
        return view(saved);
    }

    public StoredFile download(Long id) {
        LaborCaseDocument document = requireDocument(id);
        return new StoredFile(document.getFileName(), document.getContentType(), document.getFileData());
    }

    @Transactional
    public void delete(Long id) {
        LaborCaseDocument document = requireDocument(id);
        Long unitId = document.getLaborCase().getUnionUnit().getId();
        documents.delete(document);
        events.changed("case-documents", "DELETED", id, unitId);
    }

    private Specification<LaborCaseDocument> filter(Long caseId) {
        Long unitId = currentUser.scopedUnitId(null);
        Specification<LaborCaseDocument> caseFilter = caseId == null ? null
                : (root, query, cb) -> cb.equal(root.get("laborCase").get("id"), caseId);
        return Specs.nullSafe(Specs.allOf(Specs.unitScopeVia("laborCase", unitId), caseFilter));
    }

    private LaborCase requireCase(Long id) {
        LaborCase laborCase = cases.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kiến nghị với id=" + id));
        currentUser.requireUnitAccess(laborCase.getUnionUnit().getId());
        return laborCase;
    }

    private LaborCaseDocument requireDocument(Long id) {
        LaborCaseDocument document = documents.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu kiến nghị với id=" + id));
        currentUser.requireUnitAccess(document.getLaborCase().getUnionUnit().getId());
        return document;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn tệp cần tải lên");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("Tệp không được lớn hơn 10 MB");
        String type = contentType(file).toLowerCase();
        boolean allowed = type.equals("application/pdf") || type.startsWith("image/")
                || type.equals("application/msword")
                || type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                || type.equals("application/vnd.ms-excel")
                || type.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        if (!allowed) throw new IllegalArgumentException("Chỉ hỗ trợ PDF, Word, Excel và tệp ảnh");
    }

    private LaborCaseDocumentView view(LaborCaseDocument item) {
        LaborCase laborCase = item.getLaborCase();
        return new LaborCaseDocumentView(item.getId(), laborCase.getId(), laborCase.getCaseCode(), item.getFileName(),
                item.getContentType(), item.getFileSize(), item.getUploadedBy(), item.getCreatedAt());
    }

    private byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể đọc nội dung tệp", exception);
        }
    }

    private String contentType(MultipartFile file) {
        return file.getContentType() == null ? "application/octet-stream" : file.getContentType();
    }

    private String safeFileName(String name) {
        if (name == null || name.isBlank()) return "tai-lieu-kien-nghi";
        String safe = name.replace('\\', '_').replace('/', '_').trim();
        return safe.length() <= 255 ? safe : safe.substring(safe.length() - 255);
    }

    public record StoredFile(String fileName, String contentType, byte[] data) { }
}
