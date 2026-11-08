package ru.itmo.soa.hr.dto;

import java.io.Serializable;
import java.util.List;

/** Serializable so instances can cross the EJB remote boundary. */
public record OrganizationIndexationResult(int updatedCount, List<IndexationResult> results) implements Serializable {
    private static final long serialVersionUID = 1L;

    public OrganizationIndexationResult(List<IndexationResult> results) {
        this(results.size(), results);
    }
}
