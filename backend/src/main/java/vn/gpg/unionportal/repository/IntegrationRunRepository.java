package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.IntegrationRun;

public interface IntegrationRunRepository extends JpaRepository<IntegrationRun, Long> {
}
