package vn.gpg.unionportal.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.AdminUser;

import java.util.Locale;

/**
 * Filters for the account admin table. {@code role} and {@code active} are plain columns rather than
 * domain enums, so the search matches them the way {@code UsersPage} used to: role by its raw value,
 * status by the Vietnamese words the table renders.
 */
public final class AdminUserSpecs {
    private static final Locale VI = Locale.forLanguageTag("vi");

    private AdminUserSpecs() {
    }

    public static Specification<AdminUser> filter(ListQuery query) {
        return Specs.allOf(
                status(query.statusValue()),
                search(query.text(), query.field()));
    }

    /** The status dropdown on this screen is "ACTIVE" / "INACTIVE" rather than a domain enum. */
    private static Specification<AdminUser> status(String status) {
        if (status == null) return null;
        return switch (status) {
            case "ACTIVE" -> Specs.isTrue("active");
            case "INACTIVE" -> Specs.isFalse("active");
            default -> Specs.none();
        };
    }

    private static Specification<AdminUser> search(String text, String field) {
        if (text.isEmpty()) return null;
        return (root, criteria, cb) -> switch (field) {
            case "username" -> Specs.textLike(cb, root.get("username"), text);
            case "fullName" -> Specs.textLike(cb, root.get("fullName"), text);
            case "role" -> Specs.textLike(cb, root.get("role"), text);
            case "unit" -> Specs.unitLike(cb, root.get("unionUnit"), text);
            case "active" -> activeLike(cb, root, text);
            default -> cb.or(
                    Specs.textLike(cb, root.get("username"), text),
                    Specs.textLike(cb, root.get("fullName"), text),
                    Specs.textLike(cb, root.get("role"), text),
                    Specs.unitLike(cb, root.get("unionUnit"), text),
                    activeLike(cb, root, text));
        };
    }

    /** Matches the words the table renders for the active flag, e.g. "đang hoạt động" or "đã khóa". */
    private static Predicate activeLike(CriteriaBuilder cb, Root<AdminUser> root, String text) {
        String needle = text.toLowerCase(VI);
        boolean matchesActive = "đang hoạt động active".contains(needle);
        boolean matchesInactive = "đã khóa inactive".contains(needle);
        if (matchesActive && matchesInactive) return cb.conjunction();
        if (matchesActive) return cb.isTrue(root.get("active"));
        if (matchesInactive) return cb.isFalse(root.get("active"));
        return cb.disjunction();
    }
}
