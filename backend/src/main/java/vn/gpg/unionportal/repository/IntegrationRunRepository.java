package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.gpg.unionportal.model.IntegrationRun;

public interface IntegrationRunRepository extends JpaRepository<IntegrationRun, Long>, JpaSpecificationExecutor<IntegrationRun> {
}
