package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.DomainEnums.WelfareDocumentType;
import vn.gpg.unionportal.model.WelfareDocument;

import java.util.List;

public interface WelfareDocumentRepository extends JpaRepository<WelfareDocument, Long> {
    List<WelfareDocument> findByWelfareRecordIdOrderByCreatedAtDesc(Long welfareRecordId);

    boolean existsByWelfareRecordIdAndDocumentType(Long welfareRecordId, WelfareDocumentType documentType);
}
