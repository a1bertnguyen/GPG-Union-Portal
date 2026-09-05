package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gpg.unionportal.model.FinanceDocument;

import java.util.Collection;
import java.util.List;

public interface FinanceDocumentRepository extends JpaRepository<FinanceDocument, Long> {
    List<FinanceDocument> findByFinanceEntryIdOrderByCreatedAtDesc(Long financeEntryId);

    boolean existsByFinanceEntryId(Long financeEntryId);

    /** Finance entries that actually have a stored voucher, for the KPI evidence check. */
    @Query("select distinct document.financeEntry.id from FinanceDocument document "
            + "where document.financeEntry.id in :ids")
    List<Long> findFinanceEntryIdsWithDocuments(@Param("ids") Collection<Long> ids);
}
