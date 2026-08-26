package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.gpg.unionportal.model.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {
    Optional<Member> findByEmployeeCodeIgnoreCase(String employeeCode);
}
