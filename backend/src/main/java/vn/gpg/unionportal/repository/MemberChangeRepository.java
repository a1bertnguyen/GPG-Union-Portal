package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.gpg.unionportal.model.MemberChange;

import java.util.List;

public interface MemberChangeRepository extends JpaRepository<MemberChange, Long>, JpaSpecificationExecutor<MemberChange> {
    List<MemberChange> findByMemberIdOrderByEffectiveDateDescIdDesc(Long memberId);
}
