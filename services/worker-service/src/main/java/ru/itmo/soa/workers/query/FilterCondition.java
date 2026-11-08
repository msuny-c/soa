package ru.itmo.soa.workers.query;

import lombok.Value;

/**
 * Immutable parsed filter condition (a Java 11 replacement for the previous record,
 * keeping the canonical-constructor call sites).
 */
@Value
public class FilterCondition {
    WorkerField field;
    FilterOperation operation;
    Object value;
}
