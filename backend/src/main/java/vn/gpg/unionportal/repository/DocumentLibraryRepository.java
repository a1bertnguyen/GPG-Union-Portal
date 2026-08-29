package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.gpg.unionportal.model.DocumentLibraryItem;

public interface DocumentLibraryRepository extends JpaRepository<DocumentLibraryItem, Long>,
        JpaSpecificationExecutor<DocumentLibraryItem> {
}
