package ru.itmo.soa.hr.error;

import java.util.List;
import ru.itmo.soa.hr.dto.ErrorDetail;

public class ApiException extends RuntimeException {

    private final int status;
    private final List<ErrorDetail> details;

    public ApiException(int status, String message, List<ErrorDetail> details) {
        super(message);
        this.status = status;
        this.details = details;
    }

    public int getStatus() {
        return status;
    }

    public List<ErrorDetail> getDetails() {
        return details;
    }

    public ApiException withMessage(String message) {
        return new ApiException(status, message, details);
    }
}
