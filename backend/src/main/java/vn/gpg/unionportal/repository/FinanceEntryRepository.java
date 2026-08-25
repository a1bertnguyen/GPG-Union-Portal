package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.FinanceEntry;

import java.util.Optional;

public interface FinanceEntryRepository extends JpaRepository<FinanceEntry, Long> {
    Optional<FinanceEntry> findByEntryCodeIgnoreCase(String entryCode);
}
