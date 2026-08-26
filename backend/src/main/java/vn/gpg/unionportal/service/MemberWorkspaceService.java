package vn.gpg.unionportal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.dto.ApiModels.*;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.model.*;
import vn.gpg.unionportal.model.DomainEnums.MemberDocumentType;
import vn.gpg.unionportal.repository.*;
import vn.gpg.unionportal.spec.Specs;
import vn.gpg.unionportal.spec.WorkspaceSpecs;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MemberWorkspaceService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Sort CHANGE_SORT = Sort.by(Sort.Direction.DESC, "effectiveDate", "id");
    private static final Sort DOCUMENT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");
    private static final Sort MEMBER_SORT = Sort.by("fullName");

    private final MemberRepository members;
    private final MemberChangeRepository changes;
    private final MemberDocumentRepository documents;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;

    public MemberWorkspaceService(MemberRepository members, MemberChangeRepository changes,
                                  MemberDocumentRepository documents, CurrentUserService currentUser,
                                  RealtimeEventPublisher events) {
        this.members = members;
        this.changes = changes;
        this.documents = documents;
        this.currentUser = currentUser;
        this.events = events;
    }

    public List<MemberChangeView> listChanges(Long memberId) {
        return searchChanges(ListQuery.firstPage(), memberId);
    }

    public Page<MemberChangeView> pageChanges(ListQuery query, Long memberId) {
        return changes.findAll(changeFilter(query, memberId), query.pageable(CHANGE_SORT)).map(this::changeView);
    }

    public List<MemberChangeView> searchChanges(ListQuery query, Long memberId) {
        return changes.findAll(changeFilter(query, memberId), CHANGE_SORT).stream().map(this::changeView).toList();
    }

    public ListFacets changeFacets(ListQuery query, Long memberId) {
        Specification<MemberChange> scope = Specs.nullSafe(Specs.unitScopeVia("member", scopedUnitId()));
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("total", changes.count(changeFilter(query, memberId)));
        return new ListFacets(changes.count(scope), List.of(), metrics);
    }

    private Specification<MemberChange> changeFilter(ListQuery query, Long memberId) {
        return Specs.nullSafe(WorkspaceSpecs.memberChanges(query, scopedUnitId(), memberId));
    }

    @Transactional
    public MemberChangeView createChange(MemberChangeRequest request) {
        Member member = requireMember(request.memberId());
        var change = new MemberChange();
        change.setMember(member);
        change.setChangeType(request.changeType().trim());
        change.setEffectiveDate(request.effectiveDate() == null ? LocalDate.now() : request.effectiveDate());
        change.setDescription(request.description().trim());
        change.setRecordedBy(currentUser.username());
        var saved = changes.save(change);
        events.changed("member-changes", "CREATED", saved.getId(), member.getUnionUnit().getId());
        return changeView(saved);
    }

    public List<MemberDocumentView> listDocuments(Long memberId) {
        return searchDocuments(ListQuery.firstPage(), memberId);
    }

    public Page<MemberDocumentView> pageDocuments(ListQuery query, Long memberId) {
        return documents.findAll(documentFilter(query, memberId), query.pageable(DOCUMENT_SORT))
                .map(this::documentView);
    }

    public List<MemberDocumentView> searchDocuments(ListQuery query, Long memberId) {
        return documents.findAll(documentFilter(query, memberId), DOCUMENT_SORT).stream()
                .map(this::documentView)
                .toList();
    }

    private Specification<MemberDocument> documentFilter(ListQuery query, Long memberId) {
        return Specs.nullSafe(WorkspaceSpecs.memberDocuments(query, scopedUnitId(), memberId));
    }

    /**
     * A page of members with their required-document status.
     *
     * <p>The compliance grid needs one card per member, not per document, and the "còn thiếu / đủ hồ sơ"
     * filter depends on how many document types a member has. Both are resolved in the database, then a
     * single follow-up query loads the documents for just the members on this page.
     */
    public Page<MemberComplianceView> pageCompliance(ListQuery query) {
        Page<Member> page = members.findAll(complianceFilter(query), query.pageable(MEMBER_SORT));
        Map<Long, List<MemberDocument>> byMember = documentsOf(page.getContent());
        return page.map(member -> compliance(member, byMember));
    }

    public List<MemberComplianceView> searchCompliance(ListQuery query) {
        List<Member> matching = members.findAll(complianceFilter(query), MEMBER_SORT);
        Map<Long, List<MemberDocument>> byMember = documentsOf(matching);
        return matching.stream().map(member -> compliance(member, byMember)).toList();
    }

    public ListFacets complianceFacets(ListQuery query) {
        Specification<Member> scope = Specs.nullSafe(Specs.unitScope(scopedUnitId()));
        Specification<Member> filtered = complianceFilter(query);
        ListQuery base = new ListQuery(null, null, true, query.q(), query.searchField(), query.unitId(), null, null);
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("total", members.count(filtered));
        metrics.put("missing", members.count(Specs.nullSafe(
                WorkspaceSpecs.memberCompliance(withPreset(base, "missing"), scopedUnitId()))));
        metrics.put("complete", members.count(Specs.nullSafe(
                WorkspaceSpecs.memberCompliance(withPreset(base, "complete"), scopedUnitId()))));
        return new ListFacets(members.count(scope), List.of(), metrics);
    }

    private static ListQuery withPreset(ListQuery base, String preset) {
        return new ListQuery(base.page(), base.size(), base.all(), base.q(), base.searchField(), base.unitId(),
                base.status(), preset);
    }

    private Specification<Member> complianceFilter(ListQuery query) {
        return Specs.nullSafe(WorkspaceSpecs.memberCompliance(query, scopedUnitId()));
    }

    /** One query for all documents belonging to the members on this page, grouped by member id. */
    private Map<Long, List<MemberDocument>> documentsOf(List<Member> page) {
        if (page.isEmpty()) return Map.of();
        return documents.findAll(Specs.<MemberDocument>in("member", page), DOCUMENT_SORT).stream()
                .collect(Collectors.groupingBy(document -> document.getMember().getId()));
    }

    private MemberComplianceView compliance(Member member, Map<Long, List<MemberDocument>> byMember) {
        List<MemberDocument> owned = byMember.getOrDefault(member.getId(), List.of());
        List<MemberDocumentType> missing = Arrays.stream(MemberDocumentType.values())
                .filter(type -> owned.stream().noneMatch(document -> document.getDocumentType() == type))
                .toList();
        return new MemberComplianceView(member.getId(), member.getEmployeeCode(), member.getFullName(),
                member.getUnionUnit(), owned.stream().map(this::documentView).toList(), missing);
    }

    private Long scopedUnitId() {
        return currentUser.scopedUnitId(null);
    }

    @Transactional
    public MemberDocumentView uploadDocument(Long memberId, MemberDocumentType documentType, MultipartFile file) {
        Member member = requireMember(memberId);
        validateFile(file, false);
        var document = new MemberDocument();
        document.setMember(member);
        document.setDocumentType(documentType);
        document.setFileName(safeFileName(file.getOriginalFilename()));
        document.setContentType(contentType(file));
        document.setFileSize(file.getSize());
        document.setFileData(bytes(file));
        document.setUploadedBy(currentUser.username());
        var saved = documents.save(document);
        events.changed("member-documents", "CREATED", saved.getId(), member.getUnionUnit().getId());
        return documentView(saved);
    }

    public StoredFile downloadDocument(Long id) {
        MemberDocument document = documents.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu đoàn viên với id=" + id));
        currentUser.requireUnitAccess(document.getMember().getUnionUnit().getId());
        return new StoredFile(document.getFileName(), document.getContentType(), document.getFileData());
    }

    @Transactional
    public void deleteDocument(Long id) {
        MemberDocument document = documents.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu đoàn viên với id=" + id));
        currentUser.requireUnitAccess(document.getMember().getUnionUnit().getId());
        Long unitId = document.getMember().getUnionUnit().getId();
        documents.delete(document);
        events.changed("member-documents", "DELETED", id, unitId);
    }

    private Member requireMember(Long id) {
        Member member = members.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đoàn viên với id=" + id));
        currentUser.requireUnitAccess(member.getUnionUnit().getId());
        return member;
    }

    private MemberChangeView changeView(MemberChange item) {
        Member member = item.getMember();
        return new MemberChangeView(item.getId(), member.getId(), member.getEmployeeCode(), member.getFullName(),
                member.getUnionUnit(), item.getChangeType(), item.getEffectiveDate(), item.getDescription(),
                item.getRecordedBy(), item.getCreatedAt());
    }

    private MemberDocumentView documentView(MemberDocument item) {
        Member member = item.getMember();
        return new MemberDocumentView(item.getId(), member.getId(), member.getEmployeeCode(), member.getFullName(),
                member.getUnionUnit(), item.getDocumentType(), item.getFileName(), item.getContentType(),
                item.getFileSize(), item.getUploadedBy(), item.getCreatedAt());
    }

    private void validateFile(MultipartFile file, boolean imageOnly) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn tệp cần tải lên");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("Tệp không được lớn hơn 10 MB");
        String type = contentType(file).toLowerCase();
        boolean allowed = type.equals("application/pdf") || type.startsWith("image/")
                || type.equals("application/msword")
                || type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        if (imageOnly ? !type.startsWith("image/") : !allowed) {
            throw new IllegalArgumentException(imageOnly ? "Thư viện chỉ nhận tệp ảnh" : "Chỉ hỗ trợ PDF, Word và tệp ảnh");
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
        if (name == null || name.isBlank()) return "tai-lieu";
        String safe = name.replace('\\', '_').replace('/', '_').trim();
        return safe.length() <= 255 ? safe : safe.substring(safe.length() - 255);
    }

    private String contentType(MultipartFile file) {
        return file.getContentType() == null ? "application/octet-stream" : file.getContentType();
    }

    public record StoredFile(String fileName, String contentType, byte[] data) {
    }
}
