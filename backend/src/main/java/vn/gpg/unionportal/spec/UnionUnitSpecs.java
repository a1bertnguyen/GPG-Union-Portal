package vn.gpg.unionportal.spec;

import org.springframework.data.jpa.domain.Specification;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.DomainEnums.LegalStatus;
import vn.gpg.unionportal.model.UnionUnit;

/**
 * Database-side equivalent of the CĐCS list filters.
 *
 * <p>Unit scoping works on the row's own id here rather than a {@code unionUnit} association, which
 * preserves the old {@code UnionUnitService.list()} behaviour of showing a USER only their own CĐCS.
 */
public final class UnionUnitSpecs {
    private UnionUnitSpecs() {
    }

    public static Specification<UnionUnit> filter(ListQuery query, Long scopedUnitId) {
        return Specs.allOf(
                ownScope(scopedUnitId),
                Specs.enumEquals("legalStatus", LegalStatus.class, query.statusValue()),
                search(query.text(), query.field()));
    }

    private static Specification<UnionUnit> ownScope(Long scopedUnitId) {
        if (scopedUnitId == null) return null;
        return (root, criteria, cb) -> cb.equal(root.get("id"), scopedUnitId);
    }

    private static Specification<UnionUnit> search(String text, String field) {
        if (text.isEmpty()) return null;
        return (root, criteria, cb) -> switch (field) {
            case "code" -> Specs.textLike(cb, root.get("code"), text);
            case "name" -> Specs.textLike(cb, root.get("name"), text);
            case "companyName" -> Specs.textLike(cb, root.get("companyName"), text);
            case "location" -> Specs.textLike(cb, root.get("location"), text);
            case "chairperson" -> Specs.textLike(cb, root.get("chairperson"), text);
            case "contactPerson" -> Specs.textLike(cb, root.get("contactPerson"), text);
            case "termStart" -> Specs.valueLike(cb, root.get("termStart"), text);
            case "termEnd" -> Specs.valueLike(cb, root.get("termEnd"), text);
            case "decisionNumber" -> Specs.textLike(cb, root.get("decisionNumber"), text);
            case "legalStatus" -> Specs.enumLike(cb, root.get("legalStatus"), LegalStatus.class, text);
            default -> cb.or(
                    Specs.textLike(cb, root.get("code"), text),
                    Specs.textLike(cb, root.get("name"), text),
                    Specs.textLike(cb, root.get("companyName"), text),
                    Specs.textLike(cb, root.get("location"), text),
                    Specs.textLike(cb, root.get("chairperson"), text),
                    Specs.textLike(cb, root.get("contactPerson"), text),
                    Specs.textLike(cb, root.get("decisionNumber"), text),
                    Specs.valueLike(cb, root.get("termStart"), text),
                    Specs.valueLike(cb, root.get("termEnd"), text),
                    Specs.enumLike(cb, root.get("legalStatus"), LegalStatus.class, text));
        };
    }
}
