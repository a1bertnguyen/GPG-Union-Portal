package vn.gpg.unionportal.repository;

/** Aggregate projection used to build catalogue stock views without persisting a mutable balance. */
public interface InventoryItemQuantity {
    Long getItemId();

    Long getQuantity();
}
