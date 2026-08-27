package vn.gpg.unionportal.spec;

import org.springframework.data.jpa.domain.Specification;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.DomainEnums.DocumentStatus;
import vn.gpg.unionportal.model.DomainEnums.FinanceEntryType;
import vn.gpg.unionportal.model.FinanceEntry;

/**
 * Database-side equivalent of the internal finance list filters. The status dropdown on this screen
 * filters {@code entryType} (Thu / Chi), not a workflow status — see {@code statusField} in App.tsx.
 */
public final class FinanceSpecs {
    private FinanceSpecs() {
    }

    public static Specification<FinanceEntry> filter(ListQuery query, Long scopedUnitId) {
        return Specs.allOf(
                Specs.unitScope(scopedUnitId),
                Specs.inMonth("transactionDate", query.monthValue()),
                Specs.enumEquals("entryType", FinanceEntryType.class, query.statusValue()),
                search(query.text(), query.field()));
    }

    private static Specification<FinanceEntry> search(String text, String field) {
        if (text.isEmpty()) return null;
        return (root, criteria, cb) -> switch (field) {
            case "entryCode" -> Specs.textLike(cb, root.get("entryCode"), text);
            case "unionUnitId" -> Specs.unitLike(cb, root.get("unionUnit"), text);
            case "transactionDate" -> Specs.valueLike(cb, root.get("transactionDate"), text);
            case "entryType" -> Specs.enumLike(cb, root.get("entryType"), FinanceEntryType.class, text);
            case "category" -> Specs.textLike(cb, root.get("category"), text);
            case "amount" -> Specs.valueLike(cb, root.get("amount"), text);
            case "documentNumber" -> Specs.textLike(cb, root.get("documentNumber"), text);
            case "documentStatus" -> Specs.enumLike(cb, root.get("documentStatus"), DocumentStatus.class, text);
            case "description" -> Specs.textLike(cb, root.get("description"), text);
            default -> cb.or(
                    Specs.textLike(cb, root.get("entryCode"), text),
                    Specs.textLike(cb, root.get("category"), text),
                    Specs.textLike(cb, root.get("documentNumber"), text),
                    Specs.textLike(cb, root.get("description"), text),
                    Specs.valueLike(cb, root.get("transactionDate"), text),
                    Specs.valueLike(cb, root.get("amount"), text),
                    Specs.enumLike(cb, root.get("entryType"), FinanceEntryType.class, text),
                    Specs.enumLike(cb, root.get("documentStatus"), DocumentStatus.class, text),
                    Specs.unitLike(cb, root.get("unionUnit"), text));
        };
    }
}
