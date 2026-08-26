package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.gpg.unionportal.model.MemberDocument;

import java.util.List;

public interface MemberDocumentRepository extends JpaRepository<MemberDocument, Long>, JpaSpecificationExecutor<MemberDocument> {
    List<MemberDocument> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
