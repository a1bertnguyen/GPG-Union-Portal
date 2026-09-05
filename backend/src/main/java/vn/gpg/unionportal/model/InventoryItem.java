package vn.gpg.unionportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A stock catalogue entry owned by exactly one CĐCS. */
@Entity
@Table(name = "inventory_items", uniqueConstraints =
        @UniqueConstraint(name = "uk_inventory_item_unit_code", columnNames = {"union_unit_id", "item_code"}))
@Getter
@Setter
@NoArgsConstructor
public class InventoryItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "union_unit_id", nullable = false)
    private UnionUnit unionUnit;

    @Column(name = "item_code", nullable = false, length = 60)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(length = 120)
    private String category;

    @Column(length = 200)
    private String supplier;

    @Column(name = "unit_of_measure", nullable = false, length = 40)
    private String unitOfMeasure;

    @Column(name = "minimum_stock", nullable = false)
    private int minimumStock;

    @Column(length = 1000)
    private String note;
}
