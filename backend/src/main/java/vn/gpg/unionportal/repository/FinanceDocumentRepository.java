package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.FinanceDocument;

import java.util.List;

public interface FinanceDocumentRepository extends JpaRepository<FinanceDocument, Long> {
    List<FinanceDocument> findByFinanceEntryIdOrderByCreatedAtDesc(Long financeEntryId);

    boolean existsByFinanceEntryId(Long financeEntryId);
}
