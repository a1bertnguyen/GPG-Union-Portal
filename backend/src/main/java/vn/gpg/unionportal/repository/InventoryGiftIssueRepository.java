package vn.gpg.unionportal.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gpg.unionportal.model.InventoryGiftIssue;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryGiftIssueRepository extends JpaRepository<InventoryGiftIssue, Long>, JpaSpecificationExecutor<InventoryGiftIssue> {
    long countByItem_Id(Long itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select issue from InventoryGiftIssue issue where issue.id = :id")
    Optional<InventoryGiftIssue> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select issue.item.id as itemId, coalesce(sum(issue.quantity), 0) as quantity
            from InventoryGiftIssue issue
            where issue.item.id in :itemIds
            group by issue.item.id
            """)
    List<InventoryItemQuantity> totalQuantityByItemIds(@Param("itemIds") Collection<Long> itemIds);
}
