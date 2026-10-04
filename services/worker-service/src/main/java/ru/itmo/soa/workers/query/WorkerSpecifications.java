package ru.itmo.soa.workers.query;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import ru.itmo.soa.workers.domain.Worker;

@UtilityClass
public class WorkerSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    public static Specification<Worker> matching(List<FilterCondition> conditions) {
        return (root, query, cb) -> cb.and(conditions.stream()
                .map(condition -> toPredicate(condition.field().path(root), condition, cb))
                .toArray(Predicate[]::new));
    }

    public static Specification<Worker> nameContains(String substring) {
        return (root, query, cb) -> contains(WorkerField.NAME.path(root), substring, cb);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate toPredicate(Path path, FilterCondition condition, CriteriaBuilder cb) {
        Object value = condition.value();
        return switch (condition.operation()) {
            case EQ -> cb.equal(path, value);
            case NE -> cb.or(cb.notEqual(path, value), cb.isNull(path));
            case GT -> cb.greaterThan(path, (Comparable) value);
            case GTE -> cb.greaterThanOrEqualTo(path, (Comparable) value);
            case LT -> cb.lessThan(path, (Comparable) value);
            case LTE -> cb.lessThanOrEqualTo(path, (Comparable) value);
            case SUBSTR -> contains(path, (String) value, cb);
            case NULL -> Boolean.TRUE.equals(value) ? cb.isNull(path) : cb.isNotNull(path);
        };
    }

    private static Predicate contains(Path<String> path, String substring, CriteriaBuilder cb) {
        String escaped = substring
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return cb.like(path, "%" + escaped + "%", LIKE_ESCAPE);
    }
}
