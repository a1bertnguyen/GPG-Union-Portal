package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.gpg.unionportal.model.UnionActivity;

import java.util.Optional;

public interface UnionActivityRepository extends JpaRepository<UnionActivity, Long>, JpaSpecificationExecutor<UnionActivity> {
    Optional<UnionActivity> findByActivityCodeIgnoreCase(String activityCode);
}
