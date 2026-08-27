package vn.gpg.unionportal.spec;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.DomainEnums.CaseSeverity;
import vn.gpg.unionportal.model.DomainEnums.CaseStatus;
import vn.gpg.unionportal.model.LaborCase;

import java.time.LocalDate;

/** Database-side equivalent of the labor case list filters and its four tracking presets. */
public final class LaborCaseSpecs {
    /** "Ảnh hưởng nhiều NLĐ" threshold, matching {@code casePresetFilters} in the frontend. */
    private static final int WIDE_IMPACT_THRESHOLD = 10;

    private LaborCaseSpecs() {
    }

    public static Specification<LaborCase> filter(ListQuery query, Long scopedUnitId, LocalDate today) {
        return Specs.allOf(
                Specs.unitScope(scopedUnitId),
                Specs.inMonth("receivedDate", query.monthValue()),
                Specs.enumEquals("status", CaseStatus.class, query.statusValue()),
                preset(query.presetValue(), scopedUnitId, today),
                search(query.text(), query.field()));
    }

    private static Specification<LaborCase> preset(String preset, Long scopedUnitId, LocalDate today) {
        if (preset == null) return null;
        return switch (preset) {
            case "due24" -> (root, criteria, cb) -> cb.and(
                    cb.notEqual(root.get("status"), CaseStatus.CLOSED),
                    cb.between(root.get("deadline"), today, today.plusDays(1)));
            case "overdue" -> (root, criteria, cb) -> cb.and(
                    cb.notEqual(root.get("status"), CaseStatus.CLOSED),
                    cb.lessThan(root.get("deadline"), today));
            case "many" -> (root, criteria, cb) ->
                    cb.greaterThanOrEqualTo(root.get("affectedPeople"), WIDE_IMPACT_THRESHOLD);
            case "repeated" -> repeated(scopedUnitId);
            default -> null;
        };
    }

    /**
     * "Vụ việc lặp lại": the issue group appears more than once. Scoped to the caller's own CĐCS when
     * they have one, so a USER sees the same result the old client-side filter produced.
     */
    public static Specification<LaborCase> repeated(Long scopedUnitId) {
        return (root, criteria, cb) -> {
            Subquery<Long> sameGroup = criteria.subquery(Long.class);
            Root<LaborCase> other = sameGroup.from(LaborCase.class);
            sameGroup.select(cb.count(other));
            if (scopedUnitId == null) {
                sameGroup.where(cb.equal(other.get("issueGroup"), root.get("issueGroup")));
            } else {
                sameGroup.where(
                        cb.equal(other.get("issueGroup"), root.get("issueGroup")),
                        cb.equal(other.get("unionUnit").get("id"), scopedUnitId));
            }
            return cb.greaterThan(sameGroup, 1L);
        };
    }

    private static Specification<LaborCase> search(String text, String field) {
        if (text.isEmpty()) return null;
        return (root, criteria, cb) -> switch (field) {
            case "caseCode" -> Specs.textLike(cb, root.get("caseCode"), text);
            case "receivedDate" -> Specs.valueLike(cb, root.get("receivedDate"), text);
            case "unionUnitId" -> Specs.unitLike(cb, root.get("unionUnit"), text);
            case "requesterName" -> Specs.textLike(cb, root.get("requesterName"), text);
            case "source" -> Specs.textLike(cb, root.get("source"), text);
            case "issueGroup" -> Specs.textLike(cb, root.get("issueGroup"), text);
            case "severity" -> Specs.enumLike(cb, root.get("severity"), CaseSeverity.class, text);
            case "ownerName" -> Specs.textLike(cb, root.get("ownerName"), text);
            case "deadline" -> Specs.valueLike(cb, root.get("deadline"), text);
            case "status" -> Specs.enumLike(cb, root.get("status"), CaseStatus.class, text);
            case "affectedPeople" -> Specs.valueLike(cb, root.get("affectedPeople"), text);
            case "description" -> Specs.textLike(cb, root.get("description"), text);
            case "attachmentNote" -> Specs.textLike(cb, root.get("attachmentNote"), text);
            case "resultText" -> Specs.textLike(cb, root.get("resultText"), text);
            case "overdueReason" -> Specs.textLike(cb, root.get("overdueReason"), text);
            default -> cb.or(
                    Specs.textLike(cb, root.get("caseCode"), text),
                    Specs.textLike(cb, root.get("requesterName"), text),
                    Specs.textLike(cb, root.get("source"), text),
                    Specs.textLike(cb, root.get("issueGroup"), text),
                    Specs.textLike(cb, root.get("ownerName"), text),
                    Specs.textLike(cb, root.get("description"), text),
                    Specs.textLike(cb, root.get("attachmentNote"), text),
                    Specs.textLike(cb, root.get("resultText"), text),
                    Specs.textLike(cb, root.get("overdueReason"), text),
                    Specs.valueLike(cb, root.get("receivedDate"), text),
                    Specs.valueLike(cb, root.get("deadline"), text),
                    Specs.valueLike(cb, root.get("affectedPeople"), text),
                    Specs.enumLike(cb, root.get("severity"), CaseSeverity.class, text),
                    Specs.enumLike(cb, root.get("status"), CaseStatus.class, text),
                    Specs.unitLike(cb, root.get("unionUnit"), text));
        };
    }
}
