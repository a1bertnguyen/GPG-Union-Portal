package vn.gpg.unionportal.spec;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.DomainEnums.EmploymentStatus;
import vn.gpg.unionportal.model.DomainEnums.MembershipStatus;
import vn.gpg.unionportal.model.Member;

/** Database-side equivalent of the member list filters that used to run in {@code CrudPage}. */
public final class MemberSpecs {
    /** Columns the "dữ liệu còn thiếu" preset treats as required — mirrors {@code memberPresetFilters}. */
    private static final String[] REQUIRED_FIELDS = {
            "company", "jobTitle", "workplace", "startWorkDate", "joinDate", "email", "phone"
    };

    private MemberSpecs() {
    }

    /** Same required-profile contract used by the list preset and DATA01. */
    public static boolean hasRequiredProfileFields(Member member) {
        return present(member.getEmployeeCode()) && present(member.getFullName())
                && member.getUnionUnit() != null && present(member.getCompany())
                && present(member.getJobTitle()) && present(member.getWorkplace())
                && member.getStartWorkDate() != null && member.getJoinDate() != null
                && present(member.getEmail()) && present(member.getPhone())
                && member.getMembershipStatus() != null && member.getEmploymentStatus() != null;
    }

    public static Specification<Member> filter(ListQuery query, Long scopedUnitId) {
        return Specs.allOf(
                Specs.unitScope(scopedUnitId),
                Specs.enumEquals("membershipStatus", MembershipStatus.class, query.statusValue()),
                preset(query.presetValue()),
                search(query.text(), query.field()));
    }

    private static Specification<Member> preset(String preset) {
        if (preset == null) return null;
        if (!preset.equals("missing")) return null;
        return (root, criteria, cb) -> {
            Predicate[] missing = new Predicate[REQUIRED_FIELDS.length];
            for (int index = 0; index < REQUIRED_FIELDS.length; index++) {
                missing[index] = Specs.isMissing(cb, root.get(REQUIRED_FIELDS[index]));
            }
            return cb.or(missing);
        };
    }

    private static Specification<Member> search(String text, String field) {
        if (text.isEmpty()) return null;
        return (root, criteria, cb) -> switch (field) {
            case "employeeCode" -> Specs.textLike(cb, root.get("employeeCode"), text);
            case "fullName" -> Specs.textLike(cb, root.get("fullName"), text);
            case "unionUnitId" -> Specs.unitLike(cb, root.get("unionUnit"), text);
            case "jobTitle" -> Specs.textLike(cb, root.get("jobTitle"), text);
            case "workplace" -> Specs.textLike(cb, root.get("workplace"), text);
            case "joinDate" -> Specs.valueLike(cb, root.get("joinDate"), text);
            case "membershipStatus" -> Specs.enumLike(cb, root.get("membershipStatus"), MembershipStatus.class, text);
            case "employmentStatus" -> Specs.enumLike(cb, root.get("employmentStatus"), EmploymentStatus.class, text);
            case "email" -> Specs.textLike(cb, root.get("email"), text);
            case "phone" -> Specs.textLike(cb, root.get("phone"), text);
            default -> cb.or(
                    Specs.textLike(cb, root.get("employeeCode"), text),
                    Specs.textLike(cb, root.get("fullName"), text),
                    Specs.textLike(cb, root.get("jobTitle"), text),
                    Specs.textLike(cb, root.get("workplace"), text),
                    Specs.textLike(cb, root.get("email"), text),
                    Specs.textLike(cb, root.get("phone"), text),
                    Specs.valueLike(cb, root.get("joinDate"), text),
                    Specs.enumLike(cb, root.get("membershipStatus"), MembershipStatus.class, text),
                    Specs.enumLike(cb, root.get("employmentStatus"), EmploymentStatus.class, text),
                    Specs.unitLike(cb, root.get("unionUnit"), text));
        };
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
