package vn.gpg.unionportal.spec;

import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.DomainEnums.DocumentStatus;
import vn.gpg.unionportal.model.DomainEnums.WelfareType;
import vn.gpg.unionportal.model.DomainEnums.WorkStatus;
import vn.gpg.unionportal.model.WelfareRecord;

import java.time.LocalDate;

/** Database-side equivalent of the welfare list filters, including the "đến hạn" tracking preset. */
public final class WelfareSpecs {
    private WelfareSpecs() {
    }

    public static Specification<WelfareRecord> filter(ListQuery query, Long scopedUnitId, LocalDate today) {
        return Specs.allOf(
                Specs.unitScope(scopedUnitId),
                Specs.inMonth("eventDate", query.monthValue()),
                Specs.enumEquals("status", WorkStatus.class, query.statusValue()),
                preset(query.presetValue(), today),
                search(query.text(), query.field()));
    }

    private static Specification<WelfareRecord> preset(String preset, LocalDate today) {
        if (preset == null) return null;
        return switch (preset) {
            // "Đến hạn": not finished yet and the due date (deadline, else the event date) is today or tomorrow.
            case "due" -> (root, criteria, cb) -> {
                Expression<LocalDate> dueDate = cb.coalesce(root.get("deadline"), root.get("eventDate"));
                return cb.and(
                        cb.notEqual(root.get("status"), WorkStatus.COMPLETED),
                        cb.lessThanOrEqualTo(dueDate, today.plusDays(1)));
            };
            case "new" -> (root, criteria, cb) -> cb.equal(root.get("status"), WorkStatus.PENDING_APPROVAL);
            default -> null;
        };
    }

    private static Specification<WelfareRecord> search(String text, String field) {
        if (text.isEmpty()) return null;
        return (root, criteria, cb) -> switch (field) {
            case "recordCode" -> Specs.textLike(cb, root.get("recordCode"), text);
            case "welfareType" -> Specs.enumLike(cb, root.get("welfareType"), WelfareType.class, text);
            case "policyName" -> Specs.textLike(cb, root.get("policyName"), text);
            case "unionUnitId" -> Specs.unitLike(cb, root.get("unionUnit"), text);
            case "beneficiaryName" -> Specs.textLike(cb, root.get("beneficiaryName"), text);
            case "eventDate" -> Specs.valueLike(cb, root.get("eventDate"), text);
            case "deadline" -> Specs.valueLike(cb, root.get("deadline"), text);
            case "status" -> Specs.enumLike(cb, root.get("status"), WorkStatus.class, text);
            case "amount" -> Specs.valueLike(cb, root.get("amount"), text);
            case "standardAmount" -> Specs.valueLike(cb, root.get("standardAmount"), text);
            case "documentStatus" -> Specs.enumLike(cb, root.get("documentStatus"), DocumentStatus.class, text);
            case "receiptStatus" -> Specs.enumLike(cb, root.get("receiptStatus"), DocumentStatus.class, text);
            case "notes" -> Specs.textLike(cb, root.get("notes"), text);
            default -> cb.or(
                    Specs.textLike(cb, root.get("recordCode"), text),
                    Specs.textLike(cb, root.get("policyName"), text),
                    Specs.textLike(cb, root.get("beneficiaryName"), text),
                    Specs.textLike(cb, root.get("notes"), text),
                    Specs.valueLike(cb, root.get("eventDate"), text),
                    Specs.valueLike(cb, root.get("deadline"), text),
                    Specs.valueLike(cb, root.get("amount"), text),
                    Specs.enumLike(cb, root.get("welfareType"), WelfareType.class, text),
                    Specs.enumLike(cb, root.get("status"), WorkStatus.class, text),
                    Specs.enumLike(cb, root.get("documentStatus"), DocumentStatus.class, text),
                    Specs.unitLike(cb, root.get("unionUnit"), text));
        };
    }
}
