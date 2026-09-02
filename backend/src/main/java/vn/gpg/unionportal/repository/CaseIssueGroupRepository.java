package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.gpg.unionportal.model.CaseIssueGroup;

public interface CaseIssueGroupRepository extends JpaRepository<CaseIssueGroup, Long>, JpaSpecificationExecutor<CaseIssueGroup> {
}
