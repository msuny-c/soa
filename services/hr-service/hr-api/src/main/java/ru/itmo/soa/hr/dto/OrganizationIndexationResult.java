package ru.itmo.soa.hr.dto;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Value;

/** Serializable and record-free — see {@link IndexationResult}. */
@Value
@AllArgsConstructor
public class OrganizationIndexationResult implements Serializable {
    private static final long serialVersionUID = 1L;

    int updatedCount;
    List<IndexationResult> results;

    public OrganizationIndexationResult(List<IndexationResult> results) {
        this(results.size(), results);
    }
}
