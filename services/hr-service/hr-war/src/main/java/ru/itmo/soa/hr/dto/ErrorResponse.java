package ru.itmo.soa.hr.dto;

import java.util.List;

public record ErrorResponse(String message, List<ErrorDetail> details) {
}
