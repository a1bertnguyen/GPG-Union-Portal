package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.WelfareRecord;

import java.util.Optional;

public interface WelfareRecordRepository extends JpaRepository<WelfareRecord, Long> {
    Optional<WelfareRecord> findByRecordCodeIgnoreCase(String recordCode);
}
