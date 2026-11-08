package ru.itmo.soa.hr.dto;

import java.io.Serializable;
import lombok.Value;

/** Serializable and record-free — see {@link IndexationResult}. */
@Value
public class ErrorDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    String field;
    String issue;
}
