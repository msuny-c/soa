package ru.itmo.soa.workers.query;

public record FilterCondition(WorkerField field, FilterOperation operation, Object value) {
}
