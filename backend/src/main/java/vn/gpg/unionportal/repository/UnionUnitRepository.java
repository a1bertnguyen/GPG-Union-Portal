package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.gpg.unionportal.model.UnionUnit;

import java.util.Optional;

public interface UnionUnitRepository extends JpaRepository<UnionUnit, Long>, JpaSpecificationExecutor<UnionUnit> {
    Optional<UnionUnit> findByCodeIgnoreCase(String code);
}
