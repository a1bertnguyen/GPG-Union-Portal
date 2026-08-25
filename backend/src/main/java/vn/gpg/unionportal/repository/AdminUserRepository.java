package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.AdminUser;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    long countByRoleAndActiveTrue(String role);
}
