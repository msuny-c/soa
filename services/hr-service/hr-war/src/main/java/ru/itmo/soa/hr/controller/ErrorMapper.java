package ru.itmo.soa.hr.controller;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.itmo.soa.hr.dto.ErrorDetail;
import ru.itmo.soa.hr.dto.ErrorResponse;
import ru.itmo.soa.hr.error.ApiException;

@Provider
public class ErrorMapper implements ExceptionMapper<Throwable> {

    private static final Logger log = Logger.getLogger(ErrorMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof ApiException api) {
            return respond(api.getStatus(), api.getMessage(), api.getDetails());
        }
        if (exception instanceof ProcessingException processing) {
            return respond(502, "Не удалось подключиться к Worker Collection Service",
                    List.of(new ErrorDetail("workerService", String.valueOf(processing.getMessage()))));
        }
        if (exception instanceof WebApplicationException web) {
            Response.StatusType status = web.getResponse().getStatusInfo();
            return respond(status.getStatusCode(), status.getReasonPhrase(), List.of());
        }
        log.log(Level.SEVERE, "Unhandled exception", exception);
        return respond(500, "Внутренняя ошибка HR Service при индексации зарплаты", List.of());
    }

    static Response respond(int status, String message, List<ErrorDetail> details) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorResponse(message, details.isEmpty() ? null : details))
                .build();
    }
}
