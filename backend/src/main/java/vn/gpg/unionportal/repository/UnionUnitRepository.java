package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gpg.unionportal.model.UnionUnit;

import java.util.Optional;

public interface UnionUnitRepository extends JpaRepository<UnionUnit, Long> {
    Optional<UnionUnit> findByCodeIgnoreCase(String code);
}
