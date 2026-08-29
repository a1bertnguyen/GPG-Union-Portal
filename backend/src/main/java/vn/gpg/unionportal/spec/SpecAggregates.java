package vn.gpg.unionportal.spec;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import vn.gpg.unionportal.dto.ApiModels.CaseGroupCount;
import vn.gpg.unionportal.model.DomainEnums.CaseStatus;
import vn.gpg.unionportal.model.DomainEnums.DocumentStatus;
import vn.gpg.unionportal.model.DomainEnums.FinanceEntryType;
import vn.gpg.unionportal.model.FinanceEntry;
import vn.gpg.unionportal.model.LaborCase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates that {@code JpaSpecificationExecutor} does not offer — sums, distinct values and
 * group-by rollups that still have to honour a {@link Specification}.
 *
 * <p>Used to build {@code ListFacets} so the metric cards stay whole-dataset accurate without
 * loading every row into memory, which is the whole point of paginating in the first place.
 */
@Component
public class SpecAggregates {
    private final EntityManager entityManager;

    public SpecAggregates(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public record CountMetrics(long total, Map<String, Long> values) {
        public long value(String key) {
            return values.getOrDefault(key, 0L);
        }
    }

    public record FinanceTotals(BigDecimal income, BigDecimal expense, BigDecimal advance,
                                long incompleteDocuments) {
    }

    /**
     * Count a scope and several conditional subsets in one database round trip. Each metric
     * specification is evaluated as a SQL CASE expression over the already-scoped rows.
     */
    public <T> CountMetrics countMetrics(Class<T> type, Specification<T> scope,
                                         Map<String, Specification<T>> metrics) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<T> root = query.from(type);
        List<Selection<?>> selections = new ArrayList<>();
        selections.add(cb.count(root));

        for (Specification<T> metric : metrics.values()) {
            Predicate predicate = metric == null ? cb.conjunction() : metric.toPredicate(root, query, cb);
            Expression<Long> flag = cb.<Long>selectCase()
                    .when(predicate == null ? cb.conjunction() : predicate, 1L)
                    .otherwise(0L);
            selections.add(cb.coalesce(cb.sum(flag), 0L));
        }

        query.multiselect(selections);
        applyWhere(query, scope, root, cb);
        Object[] row = entityManager.createQuery(query).getSingleResult();

        Map<String, Long> values = new LinkedHashMap<>();
        int index = 1;
        for (String key : metrics.keySet()) values.put(key, toLong(row[index++]));
        return new CountMetrics(toLong(row[0]), Map.copyOf(values));
    }

    /** Income, total expense (including advances), advances and incomplete-document count. */
    public FinanceTotals financeTotals(Specification<FinanceEntry> scope) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<FinanceEntry> root = query.from(FinanceEntry.class);
        Expression<BigDecimal> amount = root.get("amount");
        Expression<BigDecimal> income = cb.<BigDecimal>selectCase()
                .when(cb.equal(root.get("entryType"), FinanceEntryType.INCOME), amount)
                .otherwise(BigDecimal.ZERO);
        Expression<BigDecimal> expense = cb.<BigDecimal>selectCase()
                .when(cb.or(
                        cb.equal(root.get("entryType"), FinanceEntryType.EXPENSE),
                        cb.equal(root.get("entryType"), FinanceEntryType.ADVANCE)), amount)
                .otherwise(BigDecimal.ZERO);
        Expression<BigDecimal> advance = cb.<BigDecimal>selectCase()
                .when(cb.equal(root.get("entryType"), FinanceEntryType.ADVANCE), amount)
                .otherwise(BigDecimal.ZERO);
        Expression<Long> incomplete = cb.<Long>selectCase()
                .when(cb.equal(root.get("documentStatus"), DocumentStatus.INCOMPLETE), 1L)
                .otherwise(0L);

        query.multiselect(
                cb.coalesce(cb.sum(income), BigDecimal.ZERO),
                cb.coalesce(cb.sum(expense), BigDecimal.ZERO),
                cb.coalesce(cb.sum(advance), BigDecimal.ZERO),
                cb.coalesce(cb.sum(incomplete), 0L));
        applyWhere(query, scope, root, cb);
        Object[] row = entityManager.createQuery(query).getSingleResult();
        return new FinanceTotals(
                row[0] instanceof BigDecimal value ? value : BigDecimal.ZERO,
                row[1] instanceof BigDecimal value ? value : BigDecimal.ZERO,
                row[2] instanceof BigDecimal value ? value : BigDecimal.ZERO,
                toLong(row[3]));
    }

    /** {@code SUM(field)} over the rows matching {@code spec}; zero when nothing matches. */
    public <T> BigDecimal sum(Class<T> type, Specification<T> spec, String field) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);
        Root<T> root = query.from(type);
        Expression<BigDecimal> column = root.get(field);
        query.select(cb.coalesce(cb.sum(column), BigDecimal.ZERO));
        applyWhere(query, spec, root, cb);
        BigDecimal total = entityManager.createQuery(query).getSingleResult();
        return total == null ? BigDecimal.ZERO : total;
    }

    /** {@code SUM(field)} for integer columns, e.g. the number of affected workers. */
    public <T> long sumLong(Class<T> type, Specification<T> spec, String field) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<T> root = query.from(type);
        Expression<Integer> column = root.get(field);
        query.select(cb.coalesce(cb.sumAsLong(column), 0L));
        applyWhere(query, spec, root, cb);
        Long total = entityManager.createQuery(query).getSingleResult();
        return total == null ? 0L : total;
    }

    /**
     * Distinct non-null values of an enum column, as constant names. Drives the status dropdown,
     * so the order is stable (alphabetical) rather than whatever the database returns.
     */
    public <T> List<String> distinctValues(Class<T> type, Specification<T> spec, String field) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = cb.createQuery(Object.class);
        Root<T> root = query.from(type);
        query.select(root.get(field)).distinct(true);
        applyWhere(query, spec, root, cb);
        return entityManager.createQuery(query).getResultList().stream()
                .filter(java.util.Objects::nonNull)
                .map(value -> value instanceof Enum<?> constant ? constant.name() : String.valueOf(value))
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Per-issue-group rollup for the case analytics bars: how many cases, how many workers affected
     * and how many are past their deadline. Grouping has to span the whole filtered set, not one page.
     */
    public List<CaseGroupCount> caseGroups(Specification<LaborCase> spec, LocalDate today) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<LaborCase> root = query.from(LaborCase.class);
        Expression<String> group = root.get("issueGroup");
        Expression<Integer> overdueFlag = cb.<Integer>selectCase()
                .when(cb.and(cb.notEqual(root.get("status"), CaseStatus.CLOSED),
                        cb.lessThan(root.get("deadline"), today)), 1)
                .otherwise(0);
        query.multiselect(group, cb.count(root), cb.sumAsLong(root.get("affectedPeople")), cb.sumAsLong(overdueFlag));
        query.groupBy(group);
        applyWhere(query, spec, root, cb);

        List<CaseGroupCount> groups = new ArrayList<>();
        for (Object[] row : entityManager.createQuery(query).getResultList()) {
            groups.add(new CaseGroupCount(
                    row[0] == null ? "Khác" : String.valueOf(row[0]),
                    toLong(row[1]),
                    toLong(row[2]),
                    toLong(row[3])));
        }
        groups.sort(Comparator.comparingLong(CaseGroupCount::count).reversed()
                .thenComparing(CaseGroupCount::issueGroup));
        return groups;
    }

    private <T> void applyWhere(CriteriaQuery<?> query, Specification<T> spec, Root<T> root, CriteriaBuilder cb) {
        if (spec == null) return;
        Predicate predicate = spec.toPredicate(root, query, cb);
        if (predicate != null) query.where(predicate);
    }

    private static long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
