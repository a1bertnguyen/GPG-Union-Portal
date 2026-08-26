package vn.gpg.unionportal.spec;

import org.springframework.data.jpa.domain.Specification;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.DomainEnums.ActivityStatus;
import vn.gpg.unionportal.model.DomainEnums.DocumentStatus;
import vn.gpg.unionportal.model.UnionActivity;

/** Database-side equivalent of the activity list filters. */
public final class ActivitySpecs {
    private ActivitySpecs() {
    }

    public static Specification<UnionActivity> filter(ListQuery query, Long scopedUnitId) {
        return Specs.allOf(
                Specs.unitScope(scopedUnitId),
                Specs.enumEquals("status", ActivityStatus.class, query.statusValue()),
                preset(query.presetValue()),
                search(query.text(), query.field()));
    }

    private static Specification<UnionActivity> preset(String preset) {
        if (preset == null) return null;
        return switch (preset) {
            case "running" -> (root, criteria, cb) -> cb.equal(root.get("status"), ActivityStatus.IN_PROGRESS);
            case "completed" -> (root, criteria, cb) -> cb.equal(root.get("status"), ActivityStatus.COMPLETED);
            default -> null;
        };
    }

    private static Specification<UnionActivity> search(String text, String field) {
        if (text.isEmpty()) return null;
        return (root, criteria, cb) -> switch (field) {
            case "activityCode" -> Specs.textLike(cb, root.get("activityCode"), text);
            case "name" -> Specs.textLike(cb, root.get("name"), text);
            case "unionUnitId" -> Specs.unitLike(cb, root.get("unionUnit"), text);
            case "eventDate" -> Specs.valueLike(cb, root.get("eventDate"), text);
            case "status" -> Specs.enumLike(cb, root.get("status"), ActivityStatus.class, text);
            case "plannedBudget" -> Specs.valueLike(cb, root.get("plannedBudget"), text);
            case "actualCost" -> Specs.valueLike(cb, root.get("actualCost"), text);
            case "participantCount" -> Specs.valueLike(cb, root.get("participantCount"), text);
            case "participantList" -> Specs.textLike(cb, root.get("participantList"), text);
            case "checkInCount" -> Specs.valueLike(cb, root.get("checkInCount"), text);
            case "usefulnessScore" -> Specs.valueLike(cb, root.get("usefulnessScore"), text);
            case "quickFeedback" -> Specs.textLike(cb, root.get("quickFeedback"), text);
            case "issues" -> Specs.textLike(cb, root.get("issues"), text);
            case "documentStatus" -> Specs.enumLike(cb, root.get("documentStatus"), DocumentStatus.class, text);
            case "objective" -> Specs.textLike(cb, root.get("objective"), text);
            case "followUpOwner" -> Specs.textLike(cb, root.get("followUpOwner"), text);
            case "followUpDeadline" -> Specs.valueLike(cb, root.get("followUpDeadline"), text);
            case "lessonsLearned" -> Specs.textLike(cb, root.get("lessonsLearned"), text);
            default -> cb.or(
                    Specs.textLike(cb, root.get("activityCode"), text),
                    Specs.textLike(cb, root.get("name"), text),
                    Specs.textLike(cb, root.get("objective"), text),
                    Specs.textLike(cb, root.get("participantList"), text),
                    Specs.textLike(cb, root.get("quickFeedback"), text),
                    Specs.textLike(cb, root.get("issues"), text),
                    Specs.textLike(cb, root.get("followUpOwner"), text),
                    Specs.textLike(cb, root.get("lessonsLearned"), text),
                    Specs.valueLike(cb, root.get("eventDate"), text),
                    Specs.valueLike(cb, root.get("actualCost"), text),
                    Specs.enumLike(cb, root.get("status"), ActivityStatus.class, text),
                    Specs.enumLike(cb, root.get("documentStatus"), DocumentStatus.class, text),
                    Specs.unitLike(cb, root.get("unionUnit"), text));
        };
    }
}
