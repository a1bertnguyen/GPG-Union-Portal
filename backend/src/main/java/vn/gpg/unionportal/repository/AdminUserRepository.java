package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.gpg.unionportal.model.AdminUser;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long>, JpaSpecificationExecutor<AdminUser> {
    Optional<AdminUser> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    long countByRoleAndActiveTrue(String role);
}
