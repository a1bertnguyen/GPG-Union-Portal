package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gpg.unionportal.model.DomainEnums.WelfareDocumentType;
import vn.gpg.unionportal.model.WelfareDocument;

import java.util.Collection;
import java.util.List;

public interface WelfareDocumentRepository extends JpaRepository<WelfareDocument, Long> {
    List<WelfareDocument> findByWelfareRecordIdOrderByCreatedAtDesc(Long welfareRecordId);

    boolean existsByWelfareRecordIdAndDocumentType(Long welfareRecordId, WelfareDocumentType documentType);

    /** Care records that actually have a stored file, for the KPI evidence check. */
    @Query("select distinct document.welfareRecord.id from WelfareDocument document "
            + "where document.welfareRecord.id in :ids")
    List<Long> findWelfareRecordIdsWithDocuments(@Param("ids") Collection<Long> ids);
}
