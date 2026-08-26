package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.gpg.unionportal.model.WelfareRecord;

import java.util.Optional;

public interface WelfareRecordRepository extends JpaRepository<WelfareRecord, Long>, JpaSpecificationExecutor<WelfareRecord> {
    Optional<WelfareRecord> findByRecordCodeIgnoreCase(String recordCode);
}
