package ru.itmo.soa.hr.dto;

import java.io.Serializable;

/** Serializable so instances can cross the EJB remote boundary. */
public record IndexationResult(int workerID, long oldSalary, long newSalary) implements Serializable {
    private static final long serialVersionUID = 1L;
}
