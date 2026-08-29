package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.gpg.unionportal.model.WelfarePolicy;

import java.util.Optional;

public interface WelfarePolicyRepository extends JpaRepository<WelfarePolicy, Long>, JpaSpecificationExecutor<WelfarePolicy> {
    Optional<WelfarePolicy> findByCodeIgnoreCase(String code);
}
