package vn.gpg.unionportal.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.query.criteria.JpaExpression;
import org.springframework.data.jpa.domain.Specification;
import vn.gpg.unionportal.i18n.EnumLabels;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Building blocks shared by every module's {@code *Specs} class.
 *
 * <p>Search semantics deliberately mirror what {@code CrudPage} used to do in the browser:
 * a case-insensitive substring match, no diacritic stripping, and enum columns matched by their
 * Vietnamese label as well as their constant name (see {@link EnumLabels}).
 */
public final class Specs {
    private static final Locale VI = Locale.forLanguageTag("vi");

    private Specs() {
    }

    /** Matches nothing. Used when a filter value is present but cannot possibly match. */
    public static <T> Specification<T> none() {
        return (root, query, cb) -> cb.disjunction();
    }

    /** Matches every row. The "no filter" specification for repository methods that require one. */
    public static <T> Specification<T> matchAll() {
        return (root, query, cb) -> cb.conjunction();
    }

    /** Restricts rows to one CĐCS. A null id means "no restriction" (ADMIN viewing everything). */
    public static <T> Specification<T> unitScope(Long unitId) {
        if (unitId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("unionUnit").get("id"), unitId);
    }

    /** Same as {@link #unitScope} but for entities that reach the unit through an association. */
    public static <T> Specification<T> unitScopeVia(String association, Long unitId) {
        if (unitId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(association).get("unionUnit").get("id"), unitId);
    }

    /**
     * Equality on an enum column. An unparseable value yields {@link #none()} rather than being
     * ignored — silently widening a filter would show rows the user asked to exclude.
     */
    public static <T, E extends Enum<E>> Specification<T> enumEquals(String field, Class<E> type, String value) {
        if (value == null || value.isBlank()) return null;
        E constant;
        try {
            constant = Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException ignored) {
            return none();
        }
        return (root, query, cb) -> cb.equal(root.get(field), constant);
    }

    /** Case-insensitive substring match on a text column. */
    public static Predicate textLike(CriteriaBuilder cb, Expression<String> column, String needle) {
        return cb.like(cb.lower(column), "%" + needle.toLowerCase(VI) + "%");
    }

    /** Substring match against the string form of a non-text column, e.g. {@code join_date}. */
    public static Predicate valueLike(CriteriaBuilder cb, Path<?> column, String needle) {
        return textLike(cb, asText(cb, column), needle);
    }

    /**
     * Matches an enum column when the search text hits a constant name or its Vietnamese label,
     * so "Chờ duyệt" still finds {@code PENDING_APPROVAL}.
     */
    public static <E extends Enum<E>> Predicate enumLike(CriteriaBuilder cb, Path<E> column,
                                                         Class<E> type, String needle) {
        List<E> candidates = EnumLabels.matching(type, needle);
        return candidates.isEmpty() ? cb.disjunction() : column.in(candidates);
    }

    /** Substring match across the unit's code, name and company — the "CĐCS" search field. */
    public static Predicate unitLike(CriteriaBuilder cb, Path<?> unit, String needle) {
        return cb.or(
                textLike(cb, unit.get("code"), needle),
                textLike(cb, unit.get("name"), needle),
                textLike(cb, unit.get("companyName"), needle));
    }

    /** True when the column is null or an empty string — the "dữ liệu còn thiếu" preset. */
    public static Predicate isMissing(CriteriaBuilder cb, Path<?> column) {
        if (column.getJavaType() == String.class) {
            @SuppressWarnings("unchecked")
            Path<String> text = (Path<String>) column;
            return cb.or(cb.isNull(text), cb.equal(cb.trim(text), ""));
        }
        return cb.isNull(column);
    }

    /** Combines the non-null parts with AND. Returns null when nothing constrains the query. */
    @SafeVarargs
    public static <T> Specification<T> allOf(Specification<T>... parts) {
        List<Specification<T>> present = new ArrayList<>();
        for (Specification<T> part : parts) {
            if (part != null) present.add(part);
        }
        if (present.isEmpty()) return null;
        Specification<T> combined = present.getFirst();
        for (Specification<T> part : present.subList(1, present.size())) {
            combined = combined.and(part);
        }
        return combined;
    }

    /**
     * Never-null specification for the repository methods that require one — an absent filter
     * becomes "match everything".
     */
    public static <T> Specification<T> nullSafe(Specification<T> spec) {
        return spec == null ? matchAll() : spec;
    }

    // --- Small predicates used to express the metric cards, kept here so services read declaratively ---

    public static <T> Specification<T> eq(String field, Object value) {
        return (root, query, cb) -> cb.equal(root.get(field), value);
    }

    public static <T> Specification<T> notEq(String field, Object value) {
        return (root, query, cb) -> cb.notEqual(root.get(field), value);
    }

    public static <T> Specification<T> in(String field, Collection<?> values) {
        if (values.isEmpty()) return none();
        return (root, query, cb) -> root.get(field).in(values);
    }

    public static <T> Specification<T> notIn(String field, Collection<?> values) {
        if (values.isEmpty()) return null;
        return (root, query, cb) -> cb.not(root.get(field).in(values));
    }

    /** Column has a real value — neither null nor blank. */
    public static <T> Specification<T> isPresent(String field) {
        return (root, query, cb) -> cb.not(isMissing(cb, root.get(field)));
    }

    public static <T> Specification<T> isTrue(String field) {
        return (root, query, cb) -> cb.isTrue(root.get(field));
    }

    public static <T> Specification<T> isFalse(String field) {
        return (root, query, cb) -> cb.isFalse(root.get(field));
    }

    public static <T> Specification<T> onOrBefore(String field, LocalDate date) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get(field), date);
    }

    public static <T> Specification<T> before(String field, LocalDate date) {
        return (root, query, cb) -> cb.lessThan(root.get(field), date);
    }

    public static <T> Specification<T> atLeast(String field, int value) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(field), value);
    }

    /** Renders any path as text so it can take part in a LIKE comparison. */
    private static Expression<String> asText(CriteriaBuilder cb, Path<?> column) {
        if (column.getJavaType() == String.class) {
            @SuppressWarnings("unchecked")
            Path<String> text = (Path<String>) column;
            return text;
        }
        return ((JpaExpression<?>) column).cast(String.class);
    }
}
