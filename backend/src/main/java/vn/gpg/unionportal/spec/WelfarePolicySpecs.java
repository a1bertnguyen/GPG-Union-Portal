package vn.gpg.unionportal.spec;

import org.springframework.data.jpa.domain.Specification;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.DomainEnums.WelfarePolicySource;
import vn.gpg.unionportal.model.DomainEnums.WelfareType;
import vn.gpg.unionportal.model.WelfarePolicy;

public final class WelfarePolicySpecs {
    private WelfarePolicySpecs() {
    }

    public static Specification<WelfarePolicy> filter(ListQuery query) {
        return Specs.allOf(status(query.statusValue()), search(query.text(), query.field()));
    }

    private static Specification<WelfarePolicy> status(String value) {
        if (value == null) return null;
        if ("ACTIVE".equalsIgnoreCase(value)) return Specs.isTrue("active");
        if ("INACTIVE".equalsIgnoreCase(value)) return Specs.isFalse("active");
        return Specs.none();
    }

    private static Specification<WelfarePolicy> search(String text, String field) {
        if (text.isBlank()) return null;
        return (root, criteria, cb) -> switch (field) {
            case "code" -> Specs.textLike(cb, root.get("code"), text);
            case "source" -> Specs.enumLike(cb, root.get("source"), WelfarePolicySource.class, text);
            case "sequenceNumber" -> Specs.valueLike(cb, root.get("sequenceNumber"), text);
            case "welfareType" -> Specs.enumLike(cb, root.get("welfareType"), WelfareType.class, text);
            case "name" -> Specs.textLike(cb, root.get("name"), text);
            case "supportAmount" -> Specs.valueLike(cb, root.get("supportAmount"), text);
            case "eligibilityNotes" -> Specs.textLike(cb, root.get("eligibilityNotes"), text);
            case "processingWeeks" -> Specs.valueLike(cb, root.get("processingWeeks"), text);
            case "active" -> "đang áp dụng".contains(text.toLowerCase()) || "active".contains(text.toLowerCase())
                    ? cb.isTrue(root.get("active")) : cb.isFalse(root.get("active"));
            default -> cb.or(
                    Specs.textLike(cb, root.get("code"), text),
                    Specs.enumLike(cb, root.get("source"), WelfarePolicySource.class, text),
                    Specs.enumLike(cb, root.get("welfareType"), WelfareType.class, text),
                    Specs.textLike(cb, root.get("name"), text),
                    Specs.valueLike(cb, root.get("supportAmount"), text),
                    Specs.textLike(cb, root.get("eligibilityNotes"), text),
                    Specs.valueLike(cb, root.get("processingWeeks"), text));
        };
    }
}
