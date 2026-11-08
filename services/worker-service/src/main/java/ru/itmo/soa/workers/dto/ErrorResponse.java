package ru.itmo.soa.workers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Value;

@Value
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ErrorResponse {
    String message;
    List<ErrorDetail> details;
}
