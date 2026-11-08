package ru.itmo.soa.workers.dto;

import lombok.Value;

@Value
public class ErrorDetail {
    String field;
    String issue;
}
