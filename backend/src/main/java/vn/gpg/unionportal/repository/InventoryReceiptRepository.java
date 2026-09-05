package vn.gpg.unionportal.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gpg.unionportal.model.InventoryReceipt;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryReceiptRepository extends JpaRepository<InventoryReceipt, Long>, JpaSpecificationExecutor<InventoryReceipt> {
    long countByItem_Id(Long itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select receipt from InventoryReceipt receipt where receipt.id = :id")
    Optional<InventoryReceipt> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select receipt.item.id as itemId, coalesce(sum(receipt.quantity), 0) as quantity
            from InventoryReceipt receipt
            where receipt.item.id in :itemIds
            group by receipt.item.id
            """)
    List<InventoryItemQuantity> totalQuantityByItemIds(@Param("itemIds") Collection<Long> itemIds);
}
