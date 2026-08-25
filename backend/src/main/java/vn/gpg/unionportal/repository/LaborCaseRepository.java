package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.LaborCase;

import java.util.Optional;

public interface LaborCaseRepository extends JpaRepository<LaborCase, Long> {
    Optional<LaborCase> findByCaseCodeIgnoreCase(String caseCode);
}
