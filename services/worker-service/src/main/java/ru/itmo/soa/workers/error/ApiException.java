package ru.itmo.soa.workers.error;

import java.util.List;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import ru.itmo.soa.workers.dto.ErrorDetail;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final List<ErrorDetail> details;

    public ApiException(HttpStatus status, String message, List<ErrorDetail> details) {
        super(message);
        this.status = status;
        this.details = details;
    }

    public static ApiException badRequest(String message, String field, String issue) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, List.of(new ErrorDetail(field, issue)));
    }

    public static ApiException unprocessable(String message, String field, String issue) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, message, List.of(new ErrorDetail(field, issue)));
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message, List.of());
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message, List.of());
    }
}
