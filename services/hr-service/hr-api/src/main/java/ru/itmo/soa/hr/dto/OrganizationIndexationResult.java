package ru.itmo.soa.hr.dto;

import java.util.List;

public record OrganizationIndexationResult(int updatedCount, List<IndexationResult> results) {

    public OrganizationIndexationResult(List<IndexationResult> results) {
        this(results.size(), results);
    }
}
