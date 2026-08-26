package vn.gpg.unionportal.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Common query parameters shared by every paginated list endpoint. Bound with {@code @ModelAttribute}
 * so controllers stay readable instead of repeating eight {@code @RequestParam} declarations each.
 *
 * <p>All accessors normalise their field, so callers never have to null-check: a missing {@code page}
 * means the first page, a missing {@code size} means {@link #DEFAULT_SIZE}.
 */
public record ListQuery(
        Integer page,
        Integer size,
        Boolean all,
        String q,
        String searchField,
        Long unitId,
        String status,
        String preset) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 200;

    /** Empty query — first page, default size, no filters. Handy for internal callers and tests. */
    public static ListQuery firstPage() {
        return new ListQuery(null, null, null, null, null, null, null, null);
    }

    /** Unpaged query scoped to one unit, for internal callers such as the Excel export. */
    public static ListQuery allForUnit(Long unitId) {
        return new ListQuery(null, null, true, null, null, unitId, null, null);
    }

    /** {@code true} when the caller wants the whole list in one page (dropdown lookups). */
    public boolean fetchAll() {
        return Boolean.TRUE.equals(all);
    }

    public int pageNumber() {
        return page == null || page < 0 ? 0 : page;
    }

    public int pageSize() {
        if (size == null || size < 1) return DEFAULT_SIZE;
        return Math.min(size, MAX_SIZE);
    }

    /** Trimmed search text, never null. Empty means "no text filter". */
    public String text() {
        return q == null ? "" : q.trim();
    }

    /** Field the search box is scoped to, or {@code "all"} for every searchable column. */
    public String field() {
        return searchField == null || searchField.isBlank() ? "all" : searchField.trim();
    }

    public String statusValue() {
        return status == null || status.isBlank() ? null : status.trim();
    }

    public String presetValue() {
        return preset == null || preset.isBlank() ? null : preset.trim();
    }

    public Pageable pageable(Sort sort) {
        return PageRequest.of(pageNumber(), pageSize(), sort);
    }

    /** Same filters, but forced to return everything — used to build whole-dataset facets. */
    public ListQuery withoutPaging() {
        return new ListQuery(null, null, true, q, searchField, unitId, status, preset);
    }
}
