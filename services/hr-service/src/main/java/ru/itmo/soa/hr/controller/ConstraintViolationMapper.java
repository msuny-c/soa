package ru.itmo.soa.hr.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.stream.StreamSupport;
import ru.itmo.soa.hr.dto.ErrorDetail;

@Provider
public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        return ErrorMapper.respond(400, "Некорректные параметры запроса", exception.getConstraintViolations().stream()
                .map(violation -> new ErrorDetail(parameterName(violation),
                        violation.getMessage() + ", получено '" + violation.getInvalidValue() + "'"))
                .distinct()
                .toList());
    }

    private static String parameterName(ConstraintViolation<?> violation) {
        return StreamSupport.stream(violation.getPropertyPath().spliterator(), false)
                .reduce((first, second) -> second)
                .map(Path.Node::getName)
                .orElse("request");
    }
}
