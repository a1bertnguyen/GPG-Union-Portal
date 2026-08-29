package vn.gpg.unionportal.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.dto.ApiModels.WelfareDocumentView;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.model.DomainEnums.DocumentStatus;
import vn.gpg.unionportal.model.DomainEnums.WelfareDocumentType;
import vn.gpg.unionportal.model.DomainEnums.WorkStatus;
import vn.gpg.unionportal.model.WelfareDocument;
import vn.gpg.unionportal.model.WelfareRecord;
import vn.gpg.unionportal.repository.WelfareDocumentRepository;
import vn.gpg.unionportal.repository.WelfareRecordRepository;

import java.io.IOException;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class WelfareDocumentService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final WelfareDocumentRepository documents;
    private final WelfareRecordRepository records;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;

    public WelfareDocumentService(WelfareDocumentRepository documents, WelfareRecordRepository records,
                                  CurrentUserService currentUser, RealtimeEventPublisher events) {
        this.documents = documents;
        this.records = records;
        this.currentUser = currentUser;
        this.events = events;
    }

    public List<WelfareDocumentView> list(Long welfareRecordId) {
        requireRecord(welfareRecordId);
        return documents.findByWelfareRecordIdOrderByCreatedAtDesc(welfareRecordId).stream()
                .map(this::view)
                .toList();
    }

    @Transactional
    public WelfareDocumentView upload(Long welfareRecordId, WelfareDocumentType documentType, MultipartFile file) {
        WelfareRecord record = requireRecord(welfareRecordId);
        requireChangeAllowed(record);
        validateFile(file, documentType);

        var document = new WelfareDocument();
        document.setWelfareRecord(record);
        document.setDocumentType(documentType);
        document.setFileName(safeFileName(file.getOriginalFilename()));
        document.setContentType(contentType(file));
        document.setFileSize(file.getSize());
        document.setFileData(bytes(file));
        document.setUploadedBy(currentUser.username());
        var saved = documents.save(document);
        refreshRecordState(record);
        events.changed("welfare-documents", "CREATED", saved.getId(), record.getUnionUnit().getId());
        return view(saved);
    }

    public StoredFile download(Long id) {
        WelfareDocument document = requireDocument(id);
        return new StoredFile(document.getFileName(), document.getContentType(), document.getFileData());
    }

    @Transactional
    public void delete(Long id) {
        WelfareDocument document = requireDocument(id);
        WelfareRecord record = document.getWelfareRecord();
        requireChangeAllowed(record);
        documents.delete(document);
        documents.flush();
        refreshRecordState(record);
        events.changed("welfare-documents", "DELETED", id, record.getUnionUnit().getId());
    }

    private WelfareRecord requireRecord(Long id) {
        WelfareRecord record = records.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ chăm lo với id=" + id));
        currentUser.requireUnitAccess(record.getUnionUnit().getId());
        return record;
    }

    private WelfareDocument requireDocument(Long id) {
        WelfareDocument document = documents.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chứng từ chăm lo với id=" + id));
        currentUser.requireUnitAccess(document.getWelfareRecord().getUnionUnit().getId());
        return document;
    }

    private void requireChangeAllowed(WelfareRecord record) {
        if (!currentUser.isAdmin()
                && (record.getStatus() == WorkStatus.COMPLETED || record.getStatus() == WorkStatus.CANCELLED)) {
            throw new AccessDeniedException("Không thể thay đổi chứng từ của hồ sơ đã kết thúc");
        }
    }

    private void refreshRecordState(WelfareRecord record) {
        Long recordId = record.getId();
        record.setDocumentStatus(documents.existsByWelfareRecordIdAndDocumentType(
                recordId, WelfareDocumentType.SUPPORTING_DOCUMENT) ? DocumentStatus.COMPLETE : DocumentStatus.INCOMPLETE);
        record.setReceiptStatus(documents.existsByWelfareRecordIdAndDocumentType(
                recordId, WelfareDocumentType.RECEIPT) ? DocumentStatus.COMPLETE : DocumentStatus.INCOMPLETE);
        record.setHasImage(documents.existsByWelfareRecordIdAndDocumentType(recordId, WelfareDocumentType.IMAGE));
        records.save(record);
        events.changed("welfare", "UPDATED", recordId, record.getUnionUnit().getId());
    }

    private WelfareDocumentView view(WelfareDocument document) {
        WelfareRecord record = document.getWelfareRecord();
        return new WelfareDocumentView(document.getId(), record.getId(), record.getRecordCode(),
                document.getDocumentType(), document.getFileName(), document.getContentType(),
                document.getFileSize(), document.getUploadedBy(), document.getCreatedAt());
    }

    private void validateFile(MultipartFile file, WelfareDocumentType documentType) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn tệp cần tải lên");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("Tệp không được lớn hơn 10 MB");
        String type = contentType(file).toLowerCase();
        boolean supported = type.equals("application/pdf") || type.startsWith("image/")
                || type.equals("application/msword")
                || type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        if (!supported) throw new IllegalArgumentException("Chỉ hỗ trợ PDF, Word và tệp ảnh");
        if (documentType == WelfareDocumentType.IMAGE && !type.startsWith("image/")) {
            throw new IllegalArgumentException("Nhóm Hình ảnh chỉ nhận tệp hình ảnh");
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
        if (name == null || name.isBlank()) return "chung-tu-cham-lo";
        String safe = name.replace('\\', '_').replace('/', '_').trim();
        return safe.length() <= 255 ? safe : safe.substring(safe.length() - 255);
    }

    private String contentType(MultipartFile file) {
        return file.getContentType() == null ? "application/octet-stream" : file.getContentType();
    }

    public record StoredFile(String fileName, String contentType, byte[] data) {
    }
}
