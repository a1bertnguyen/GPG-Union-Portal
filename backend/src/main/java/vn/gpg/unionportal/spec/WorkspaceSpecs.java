package vn.gpg.unionportal.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.ActivityMedia;
import vn.gpg.unionportal.model.DomainEnums.ActivityMediaType;
import vn.gpg.unionportal.model.DomainEnums.ActivityStatus;
import vn.gpg.unionportal.model.DomainEnums.MemberDocumentType;
import vn.gpg.unionportal.model.Member;
import vn.gpg.unionportal.model.MemberChange;
import vn.gpg.unionportal.model.MemberDocument;

/**
 * Filters for the three workspace lists that hang off a parent record — member changes, member
 * documents and activity media. Each reaches its CĐCS through an association rather than owning a
 * {@code unionUnit} column, so they use {@link Specs#unitScopeVia}.
 */
public final class WorkspaceSpecs {
    private WorkspaceSpecs() {
    }

    public static Specification<MemberChange> memberChanges(ListQuery query, Long scopedUnitId, Long memberId) {
        return Specs.allOf(
                Specs.unitScopeVia("member", scopedUnitId),
                memberId == null ? null : changeOfMember(memberId),
                memberChangeSearch(query.text(), query.field()));
    }

    private static Specification<MemberChange> changeOfMember(Long memberId) {
        return (root, criteria, cb) -> cb.equal(root.get("member").get("id"), memberId);
    }

    private static Specification<MemberChange> memberChangeSearch(String text, String field) {
        if (text.isEmpty()) return null;
        return (root, criteria, cb) -> switch (field) {
            case "changeType" -> Specs.textLike(cb, root.get("changeType"), text);
            case "description" -> Specs.textLike(cb, root.get("description"), text);
            case "employeeCode" -> Specs.textLike(cb, root.get("member").get("employeeCode"), text);
            case "memberName" -> Specs.textLike(cb, root.get("member").get("fullName"), text);
            case "unit" -> Specs.unitLike(cb, root.get("member").get("unionUnit"), text);
            default -> cb.or(
                    Specs.textLike(cb, root.get("changeType"), text),
                    Specs.textLike(cb, root.get("description"), text),
                    Specs.textLike(cb, root.get("recordedBy"), text),
                    Specs.textLike(cb, root.get("member").get("employeeCode"), text),
                    Specs.textLike(cb, root.get("member").get("fullName"), text),
                    Specs.valueLike(cb, root.get("effectiveDate"), text),
                    Specs.unitLike(cb, root.get("member").get("unionUnit"), text));
        };
    }

    public static Specification<MemberDocument> memberDocuments(ListQuery query, Long scopedUnitId, Long memberId) {
        return Specs.allOf(
                Specs.unitScopeVia("member", scopedUnitId),
                memberId == null ? null : documentOfMember(memberId),
                Specs.enumEquals("documentType", MemberDocumentType.class, query.statusValue()),
                memberDocumentSearch(query.text(), query.field()));
    }

    private static Specification<MemberDocument> documentOfMember(Long memberId) {
        return (root, criteria, cb) -> cb.equal(root.get("member").get("id"), memberId);
    }

    private static Specification<MemberDocument> memberDocumentSearch(String text, String field) {
        if (text.isEmpty()) return null;
        return (root, criteria, cb) -> switch (field) {
            case "fileName" -> Specs.textLike(cb, root.get("fileName"), text);
            case "documentType" -> Specs.enumLike(cb, root.get("documentType"), MemberDocumentType.class, text);
            case "employeeCode" -> Specs.textLike(cb, root.get("member").get("employeeCode"), text);
            case "memberName" -> Specs.textLike(cb, root.get("member").get("fullName"), text);
            case "unit" -> Specs.unitLike(cb, root.get("member").get("unionUnit"), text);
            default -> cb.or(
                    Specs.textLike(cb, root.get("fileName"), text),
                    Specs.textLike(cb, root.get("uploadedBy"), text),
                    Specs.textLike(cb, root.get("member").get("employeeCode"), text),
                    Specs.textLike(cb, root.get("member").get("fullName"), text),
                    Specs.enumLike(cb, root.get("documentType"), MemberDocumentType.class, text),
                    Specs.unitLike(cb, root.get("member").get("unionUnit"), text));
        };
    }

    /**
     * @param activityId     limit to one activity, or null for all
     * @param activityStatus limit to activities in one lifecycle state (the gallery's status filter),
     *                       which is distinct from {@code query.status}, the media type
     */
    public static Specification<ActivityMedia> activityMedia(ListQuery query, Long scopedUnitId,
                                                             Long activityId, String activityStatus) {
        return Specs.allOf(
                Specs.unitScopeVia("activity", scopedUnitId),
                activityId == null ? null : mediaOfActivity(activityId),
                activityInState(activityStatus),
                Specs.enumEquals("mediaType", ActivityMediaType.class, query.statusValue()),
                activityMediaSearch(query.text(), query.field()));
    }

    private static Specification<ActivityMedia> activityInState(String activityStatus) {
        if (activityStatus == null || activityStatus.isBlank()) return null;
        ActivityStatus state;
        try {
            state = ActivityStatus.valueOf(activityStatus.trim());
        } catch (IllegalArgumentException ignored) {
            return Specs.none();
        }
        return (root, criteria, cb) -> cb.equal(root.get("activity").get("status"), state);
    }

    private static Specification<ActivityMedia> mediaOfActivity(Long activityId) {
        return (root, criteria, cb) -> cb.equal(root.get("activity").get("id"), activityId);
    }

    private static Specification<ActivityMedia> activityMediaSearch(String text, String field) {
        if (text.isEmpty()) return null;
        return (root, criteria, cb) -> switch (field) {
            case "title" -> Specs.textLike(cb, root.get("title"), text);
            case "fileName" -> Specs.textLike(cb, root.get("fileName"), text);
            case "activityCode" -> Specs.textLike(cb, root.get("activity").get("activityCode"), text);
            case "activityName" -> Specs.textLike(cb, root.get("activity").get("name"), text);
            case "unit" -> Specs.unitLike(cb, root.get("activity").get("unionUnit"), text);
            default -> cb.or(
                    Specs.textLike(cb, root.get("title"), text),
                    Specs.textLike(cb, root.get("fileName"), text),
                    Specs.textLike(cb, root.get("activity").get("activityCode"), text),
                    Specs.textLike(cb, root.get("activity").get("name"), text),
                    Specs.textLike(cb, root.get("activity").get("objective"), text),
                    Specs.unitLike(cb, root.get("activity").get("unionUnit"), text));
        };
    }

    // --- Required-document compliance: a page of members, filtered by how many document types they have ---

    /** Rows are members here, not documents, because the compliance grid shows one card per member. */
    public static Specification<Member> memberCompliance(ListQuery query, Long scopedUnitId) {
        return Specs.allOf(
                Specs.unitScope(scopedUnitId),
                complianceState(query.presetValue()),
                complianceSearch(query.text()));
    }

    private static Specification<Member> complianceState(String state) {
        if (state == null) return null;
        long required = MemberDocumentType.values().length;
        return switch (state) {
            case "missing" -> (root, criteria, cb) -> cb.lessThan(documentTypeCount(root, criteria, cb), required);
            case "complete" -> (root, criteria, cb) ->
                    cb.greaterThanOrEqualTo(documentTypeCount(root, criteria, cb), required);
            default -> null;
        };
    }

    /** How many distinct required document types this member has on file. */
    private static Expression<Long> documentTypeCount(Root<Member> member, CriteriaQuery<?> criteria,
                                                      CriteriaBuilder cb) {
        Subquery<Long> owned = criteria.subquery(Long.class);
        Root<MemberDocument> document = owned.from(MemberDocument.class);
        owned.select(cb.countDistinct(document.get("documentType")));
        owned.where(cb.equal(document.get("member").get("id"), member.get("id")));
        return owned;
    }

    /** Matches the code/name search the compliance grid offered. */
    private static Specification<Member> complianceSearch(String text) {
        if (text.isEmpty()) return null;
        return (root, criteria, cb) -> cb.or(
                Specs.textLike(cb, root.get("employeeCode"), text),
                Specs.textLike(cb, root.get("fullName"), text));
    }
}
