package vn.gpg.unionportal.spec;

import org.springframework.data.jpa.domain.Specification;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.DomainEnums.IntegrationStatus;
import vn.gpg.unionportal.model.DomainEnums.IntegrationType;
import vn.gpg.unionportal.model.IntegrationRun;

/**
 * Filters for the import history table. Runs are system-wide audit records with no CĐCS column,
 * so there is no unit scoping here — the screen is ADMIN-only.
 */
public final class IntegrationRunSpecs {
    private IntegrationRunSpecs() {
    }

    public static Specification<IntegrationRun> filter(ListQuery query) {
        return Specs.allOf(
                Specs.enumEquals("status", IntegrationStatus.class, query.statusValue()),
                search(query.text(), query.field()));
    }

    private static Specification<IntegrationRun> search(String text, String field) {
        if (text.isEmpty()) return null;
        return (root, criteria, cb) -> switch (field) {
            case "integrationType" -> Specs.enumLike(cb, root.get("integrationType"), IntegrationType.class, text);
            case "fileName" -> Specs.textLike(cb, root.get("fileName"), text);
            case "status" -> Specs.enumLike(cb, root.get("status"), IntegrationStatus.class, text);
            case "startedBy" -> Specs.textLike(cb, root.get("startedBy"), text);
            case "completedAt" -> Specs.valueLike(cb, root.get("completedAt"), text);
            case "errorSummary" -> Specs.textLike(cb, root.get("errorSummary"), text);
            default -> cb.or(
                    Specs.textLike(cb, root.get("fileName"), text),
                    Specs.textLike(cb, root.get("startedBy"), text),
                    Specs.textLike(cb, root.get("errorSummary"), text),
                    Specs.valueLike(cb, root.get("completedAt"), text),
                    Specs.enumLike(cb, root.get("integrationType"), IntegrationType.class, text),
                    Specs.enumLike(cb, root.get("status"), IntegrationStatus.class, text));
        };
    }
}
