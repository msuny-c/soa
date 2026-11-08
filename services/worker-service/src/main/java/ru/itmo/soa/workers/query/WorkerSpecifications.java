package ru.itmo.soa.workers.query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import ru.itmo.soa.workers.domain.Worker;

@UtilityClass
public class WorkerSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    public static Specification<Worker> matching(List<FilterCondition> conditions) {
        return (root, query, cb) -> cb.and(conditions.stream()
                .map(condition -> toPredicate(condition.getField().path(root), condition, cb))
                .toArray(Predicate[]::new));
    }

    public static Specification<Worker> nameContains(String substring) {
        return (root, query, cb) -> contains(WorkerField.NAME.path(root), substring, cb);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate toPredicate(Path path, FilterCondition condition, CriteriaBuilder cb) {
        Object value = condition.getValue();
        switch (condition.getOperation()) {
            case EQ:
                return cb.equal(path, value);
            case NE:
                return cb.or(cb.notEqual(path, value), cb.isNull(path));
            case GT:
                return cb.greaterThan(path, (Comparable) value);
            case GTE:
                return cb.greaterThanOrEqualTo(path, (Comparable) value);
            case LT:
                return cb.lessThan(path, (Comparable) value);
            case LTE:
                return cb.lessThanOrEqualTo(path, (Comparable) value);
            case SUBSTR:
                return contains(path, (String) value, cb);
            case NULL:
                return Boolean.TRUE.equals(value) ? cb.isNull(path) : cb.isNotNull(path);
            default:
                throw new IllegalStateException("Unexpected operation: " + condition.getOperation());
        }
    }

    private static Predicate contains(Path<String> path, String substring, CriteriaBuilder cb) {
        String escaped = substring
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return cb.like(path, "%" + escaped + "%", LIKE_ESCAPE);
    }
}
