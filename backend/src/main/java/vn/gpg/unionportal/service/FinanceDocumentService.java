package vn.gpg.unionportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.dto.ApiModels.FinanceDocumentView;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.model.DomainEnums.DocumentStatus;
import vn.gpg.unionportal.model.FinanceDocument;
import vn.gpg.unionportal.model.FinanceEntry;
import vn.gpg.unionportal.repository.FinanceDocumentRepository;
import vn.gpg.unionportal.repository.FinanceEntryRepository;

import java.io.IOException;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class FinanceDocumentService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final FinanceDocumentRepository documents;
    private final FinanceEntryRepository entries;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;

    public FinanceDocumentService(FinanceDocumentRepository documents, FinanceEntryRepository entries,
                                  CurrentUserService currentUser, RealtimeEventPublisher events) {
        this.documents = documents;
        this.entries = entries;
        this.currentUser = currentUser;
        this.events = events;
    }

    public List<FinanceDocumentView> list(Long financeEntryId) {
        requireEntry(financeEntryId);
        return documents.findByFinanceEntryIdOrderByCreatedAtDesc(financeEntryId).stream()
                .map(this::view)
                .toList();
    }

    @Transactional
    public FinanceDocumentView upload(Long financeEntryId, MultipartFile file) {
        FinanceEntry entry = requireEntry(financeEntryId);
        validateFile(file);

        var document = new FinanceDocument();
        document.setFinanceEntry(entry);
        document.setFileName(safeFileName(file.getOriginalFilename()));
        document.setContentType(contentType(file));
        document.setFileSize(file.getSize());
        document.setFileData(bytes(file));
        document.setUploadedBy(currentUser.username());
        var saved = documents.save(document);
        updateDocumentStatus(entry, DocumentStatus.COMPLETE);
        events.changed("finance-documents", "CREATED", saved.getId(), entry.getUnionUnit().getId());
        return view(saved);
    }

    public StoredFile download(Long id) {
        FinanceDocument document = requireDocument(id);
        return new StoredFile(document.getFileName(), document.getContentType(), document.getFileData());
    }

    @Transactional
    public void delete(Long id) {
        FinanceDocument document = requireDocument(id);
        FinanceEntry entry = document.getFinanceEntry();
        documents.delete(document);
        documents.flush();
        if (!documents.existsByFinanceEntryId(entry.getId())) {
            updateDocumentStatus(entry, DocumentStatus.INCOMPLETE);
        }
        events.changed("finance-documents", "DELETED", id, entry.getUnionUnit().getId());
    }

    private FinanceEntry requireEntry(Long id) {
        FinanceEntry entry = entries.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu tài chính với id=" + id));
        currentUser.requireUnitAccess(entry.getUnionUnit().getId());
        return entry;
    }

    private FinanceDocument requireDocument(Long id) {
        FinanceDocument document = documents.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chứng từ tài chính với id=" + id));
        currentUser.requireUnitAccess(document.getFinanceEntry().getUnionUnit().getId());
        return document;
    }

    private void updateDocumentStatus(FinanceEntry entry, DocumentStatus status) {
        entry.setDocumentStatus(status);
        entries.save(entry);
        events.changed("finance", "UPDATED", entry.getId(), entry.getUnionUnit().getId());
    }

    private FinanceDocumentView view(FinanceDocument document) {
        FinanceEntry entry = document.getFinanceEntry();
        return new FinanceDocumentView(document.getId(), entry.getId(), entry.getEntryCode(),
                document.getFileName(), document.getContentType(), document.getFileSize(),
                document.getUploadedBy(), document.getCreatedAt());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn chứng từ cần tải lên");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("Tệp không được lớn hơn 10 MB");
        String type = contentType(file).toLowerCase();
        boolean supported = type.equals("application/pdf") || type.startsWith("image/")
                || type.equals("application/msword")
                || type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                || type.equals("application/vnd.ms-excel")
                || type.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        if (!supported) throw new IllegalArgumentException("Chỉ hỗ trợ PDF, Word, Excel và tệp ảnh");
    }

    private byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể đọc nội dung tệp", exception);
        }
    }

    private String safeFileName(String name) {
        if (name == null || name.isBlank()) return "chung-tu-tai-chinh";
        String safe = name.replace('\\', '_').replace('/', '_').trim();
        return safe.length() <= 255 ? safe : safe.substring(safe.length() - 255);
    }

    private String contentType(MultipartFile file) {
        return file.getContentType() == null ? "application/octet-stream" : file.getContentType();
    }

    public record StoredFile(String fileName, String contentType, byte[] data) {
    }
}
