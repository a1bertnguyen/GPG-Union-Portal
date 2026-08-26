package vn.gpg.unionportal.spec;

import org.springframework.data.jpa.domain.Specification;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.DomainEnums.SurveyStatus;
import vn.gpg.unionportal.model.PulseSurvey;

/** Filters for the pulse survey table on the Employee Voice screen. */
public final class PulseSurveySpecs {
    private PulseSurveySpecs() {
    }

    public static Specification<PulseSurvey> filter(ListQuery query, Long scopedUnitId) {
        return Specs.allOf(
                Specs.unitScope(scopedUnitId),
                Specs.enumEquals("status", SurveyStatus.class, query.statusValue()),
                search(query.text(), query.field()));
    }

    private static Specification<PulseSurvey> search(String text, String field) {
        if (text.isEmpty()) return null;
        return (root, criteria, cb) -> switch (field) {
            case "surveyCode" -> Specs.textLike(cb, root.get("surveyCode"), text);
            case "title" -> Specs.textLike(cb, root.get("title"), text);
            case "questionText" -> Specs.textLike(cb, root.get("questionText"), text);
            case "unit" -> Specs.unitLike(cb, root.get("unionUnit"), text);
            case "status" -> Specs.enumLike(cb, root.get("status"), SurveyStatus.class, text);
            default -> cb.or(
                    Specs.textLike(cb, root.get("surveyCode"), text),
                    Specs.textLike(cb, root.get("title"), text),
                    Specs.textLike(cb, root.get("questionText"), text),
                    Specs.unitLike(cb, root.get("unionUnit"), text),
                    Specs.enumLike(cb, root.get("status"), SurveyStatus.class, text));
        };
    }
}
