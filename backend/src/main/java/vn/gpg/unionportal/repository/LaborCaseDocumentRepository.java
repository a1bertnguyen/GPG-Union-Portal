package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.gpg.unionportal.model.LaborCaseDocument;

public interface LaborCaseDocumentRepository extends JpaRepository<LaborCaseDocument, Long>, JpaSpecificationExecutor<LaborCaseDocument> {
    boolean existsByLaborCaseId(Long laborCaseId);
}
