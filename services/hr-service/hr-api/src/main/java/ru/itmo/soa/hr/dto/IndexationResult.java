package ru.itmo.soa.hr.dto;

import java.io.Serializable;
import lombok.Value;

/**
 * Serializable and record-free: instances cross the EJB remote boundary, and
 * GlassFish's remote-call argument copier (RMI-IIOP semantics) cannot handle records.
 */
@Value
public class IndexationResult implements Serializable {
    private static final long serialVersionUID = 1L;

    int workerID;
    long oldSalary;
    long newSalary;
}
