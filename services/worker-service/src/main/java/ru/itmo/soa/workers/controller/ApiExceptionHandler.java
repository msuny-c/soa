package ru.itmo.soa.workers.controller;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import lombok.extern.java.Log;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.itmo.soa.workers.dto.ErrorDetail;
import ru.itmo.soa.workers.dto.ErrorResponse;
import ru.itmo.soa.workers.error.ApiException;

/**
 * Unified error responses. Ported from the lab 1/2 Spring 6.1 handler: parameter
 * constraint violations now arrive as ConstraintViolationException from the
 * MethodValidationPostProcessor proxy (Spring 5.2 has no HandlerMethodValidationException).
 */
@Log
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String PARAMS_ERROR = "Некорректные параметры запроса";
    private static final String FORMAT_ERROR = "Некорректный формат тела запроса";
    private static final String CONSTRAINT_ERROR = "Нарушены ограничения полей";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApiException(ApiException ex) {
        return respond(ex.getStatus(), ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorDetail> details = ex.getConstraintViolations().stream()
                .map(ApiExceptionHandler::toDetail)
                .sorted(Comparator.comparing(ErrorDetail::getField))
                .collect(Collectors.toList());
        return respond(HttpStatus.BAD_REQUEST, PARAMS_ERROR, details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.log(Level.SEVERE, "Unhandled exception", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера при выполнении запроса", List.of());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatus status, WebRequest request) {
        return bodyViolations(ex.getBindingResult().getFieldErrors());
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatus status, WebRequest request) {
        return respond(HttpStatus.BAD_REQUEST, PARAMS_ERROR, List.of(new ErrorDetail(ex.getPropertyName(),
                "Некорректное значение '" + ex.getValue() + "'")));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatus status, WebRequest request) {
        if (ex.getCause() instanceof InvalidFormatException) {
            InvalidFormatException invalid = (InvalidFormatException) ex.getCause();
            if (invalid.getTargetType() != null && invalid.getTargetType().isEnum()) {
                return respond(HttpStatus.UNPROCESSABLE_ENTITY, CONSTRAINT_ERROR, List.of(new ErrorDetail(path(invalid),
                        "Недопустимое значение '" + invalid.getValue() + "'. Допустимые: "
                                + Arrays.stream(invalid.getTargetType().getEnumConstants()).map(Object::toString)
                                .collect(Collectors.joining(", ")))));
            }
        }
        if (ex.getCause() instanceof UnrecognizedPropertyException) {
            UnrecognizedPropertyException unrecognized = (UnrecognizedPropertyException) ex.getCause();
            return respond(HttpStatus.BAD_REQUEST, FORMAT_ERROR, List.of(new ErrorDetail(path(unrecognized), "Неизвестное поле")));
        }
        if (ex.getCause() instanceof MismatchedInputException) {
            MismatchedInputException mismatched = (MismatchedInputException) ex.getCause();
            if (mismatched.getTargetType() != null) {
                return respond(HttpStatus.BAD_REQUEST, FORMAT_ERROR, List.of(new ErrorDetail(path(mismatched),
                        "Ожидалось значение типа " + mismatched.getTargetType().getSimpleName())));
            }
        }
        return respond(HttpStatus.BAD_REQUEST, FORMAT_ERROR, List.of(new ErrorDetail("body",
                "Тело запроса отсутствует или не является корректным JSON")));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                     HttpHeaders headers, HttpStatus status,
                                                                     WebRequest request) {
        return respond(HttpStatus.BAD_REQUEST, FORMAT_ERROR, List.of(new ErrorDetail("Content-Type",
                "Ожидается application/json, получено '" + ex.getContentType() + "'")));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatus status, WebRequest request) {
        return ResponseEntity.status(status).headers(headers)
                .body(new ErrorResponse(status.getReasonPhrase(), List.of()));
    }

    private static ErrorDetail toDetail(ConstraintViolation<?> violation) {
        return new ErrorDetail(lastPathName(violation),
                violation.getMessage() + ", получено '" + violation.getInvalidValue() + "'");
    }

    private static String lastPathName(ConstraintViolation<?> violation) {
        String name = null;
        for (Path.Node node : violation.getPropertyPath()) {
            name = node.getName();
        }
        return name;
    }

    private static ResponseEntity<Object> bodyViolations(List<FieldError> errors) {
        List<ErrorDetail> details = errors.stream()
                .map(error -> new ErrorDetail(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(ErrorDetail::getField))
                .collect(Collectors.toList());
        boolean missingFields = errors.stream().anyMatch(error -> "NotNull".equals(error.getCode()));
        return missingFields
                ? respond(HttpStatus.BAD_REQUEST, FORMAT_ERROR, details)
                : respond(HttpStatus.UNPROCESSABLE_ENTITY, CONSTRAINT_ERROR, details);
    }

    private static ResponseEntity<Object> respond(HttpStatus status, String message, List<ErrorDetail> details) {
        return ResponseEntity.status(status).body(new ErrorResponse(message, details));
    }

    private static String path(JsonMappingException ex) {
        return ex.getPath().stream().map(JsonMappingException.Reference::getFieldName).collect(Collectors.joining("."));
    }
}
