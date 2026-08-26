package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.gpg.unionportal.model.LaborCase;

import java.util.Optional;

public interface LaborCaseRepository extends JpaRepository<LaborCase, Long>, JpaSpecificationExecutor<LaborCase> {
    Optional<LaborCase> findByCaseCodeIgnoreCase(String caseCode);
}
