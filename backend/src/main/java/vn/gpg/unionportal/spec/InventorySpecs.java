package vn.gpg.unionportal.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.InventoryGiftIssue;
import vn.gpg.unionportal.model.InventoryItem;
import vn.gpg.unionportal.model.InventoryReceipt;

/** Database-side filters for all inventory lists. */
public final class InventorySpecs {
    private InventorySpecs() {
    }

    public static Specification<InventoryItem> items(ListQuery query, Long scopedUnitId) {
        return Specs.allOf(Specs.unitScope(scopedUnitId), itemSearch(query.text(), query.field()));
    }

    public static Specification<InventoryReceipt> receipts(ListQuery query, Long scopedUnitId, Long itemId) {
        Specification<InventoryReceipt> item = itemId == null ? null
                : (root, criteria, cb) -> cb.equal(root.get("item").get("id"), itemId);
        return Specs.allOf(Specs.unitScope(scopedUnitId), item, Specs.inMonth("receiptDate", query.monthValue()),
                receiptSearch(query.text(), query.field()));
    }

    public static Specification<InventoryGiftIssue> issues(ListQuery query, Long scopedUnitId, Long itemId) {
        Specification<InventoryGiftIssue> item = itemId == null ? null
                : (root, criteria, cb) -> cb.equal(root.get("item").get("id"), itemId);
        return Specs.allOf(Specs.unitScope(scopedUnitId), item, Specs.inMonth("issueDate", query.monthValue()),
                issueSearch(query.text(), query.field()));
    }

    private static Specification<InventoryItem> itemSearch(String text, String field) {
        if (text.isEmpty()) return null;
        return (root, criteria, cb) -> switch (field) {
            case "itemCode" -> like(cb, root.get("itemCode"), text);
            case "itemName" -> like(cb, root.get("itemName"), text);
            case "category" -> like(cb, root.get("category"), text);
            case "supplier" -> like(cb, root.get("supplier"), text);
            case "unionUnitId" -> Specs.unitLike(cb, root.get("unionUnit"), text);
            default -> cb.or(
                    like(cb, root.get("itemCode"), text),
                    like(cb, root.get("itemName"), text),
                    like(cb, root.get("category"), text),
                    like(cb, root.get("supplier"), text),
                    Specs.unitLike(cb, root.get("unionUnit"), text));
        };
    }

    private static Specification<InventoryReceipt> receiptSearch(String text, String field) {
        if (text.isEmpty()) return null;
        return (root, criteria, cb) -> switch (field) {
            case "itemCode" -> like(cb, root.get("item").get("itemCode"), text);
            case "itemName" -> like(cb, root.get("item").get("itemName"), text);
            case "supplier" -> like(cb, root.get("supplier"), text);
            case "referenceNo" -> like(cb, root.get("referenceNo"), text);
            case "unionUnitId" -> Specs.unitLike(cb, root.get("unionUnit"), text);
            default -> cb.or(
                    like(cb, root.get("item").get("itemCode"), text),
                    like(cb, root.get("item").get("itemName"), text),
                    like(cb, root.get("supplier"), text),
                    like(cb, root.get("referenceNo"), text),
                    Specs.unitLike(cb, root.get("unionUnit"), text));
        };
    }

    private static Specification<InventoryGiftIssue> issueSearch(String text, String field) {
        if (text.isEmpty()) return null;
        return (root, criteria, cb) -> switch (field) {
            case "itemCode" -> like(cb, root.get("item").get("itemCode"), text);
            case "itemName" -> like(cb, root.get("item").get("itemName"), text);
            case "employeeCode" -> like(cb, root.get("employeeCodeSnapshot"), text);
            case "recipientName" -> like(cb, root.get("recipientNameSnapshot"), text);
            case "programName" -> like(cb, root.get("programName"), text);
            case "unionUnitId" -> Specs.unitLike(cb, root.get("unionUnit"), text);
            default -> cb.or(
                    like(cb, root.get("item").get("itemCode"), text),
                    like(cb, root.get("item").get("itemName"), text),
                    like(cb, root.get("employeeCodeSnapshot"), text),
                    like(cb, root.get("recipientNameSnapshot"), text),
                    like(cb, root.get("programName"), text),
                    Specs.unitLike(cb, root.get("unionUnit"), text));
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> Path<String> text(Path<T> path) {
        return (Path<String>) path;
    }

    private static <T> jakarta.persistence.criteria.Predicate like(CriteriaBuilder cb, Path<T> path, String value) {
        return Specs.textLike(cb, text(path), value);
    }
}
