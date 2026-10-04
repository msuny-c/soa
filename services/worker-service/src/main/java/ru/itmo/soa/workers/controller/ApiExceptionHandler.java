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
import lombok.extern.java.Log;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.itmo.soa.workers.dto.ErrorDetail;
import ru.itmo.soa.workers.dto.ErrorResponse;
import ru.itmo.soa.workers.error.ApiException;

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.log(Level.SEVERE, "Unhandled exception", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера при выполнении запроса", List.of());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        return bodyViolations(ex.getFieldErrors());
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers, HttpStatusCode status,
                                                                            WebRequest request) {
        List<ErrorDetail> paramErrors = ex.getValueResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream().map(error -> new ErrorDetail(
                        result.getMethodParameter().getParameterName(),
                        error.getDefaultMessage() + ", получено '" + result.getArgument() + "'")))
                .toList();
        if (!paramErrors.isEmpty()) {
            return respond(HttpStatus.BAD_REQUEST, PARAMS_ERROR, paramErrors);
        }
        return bodyViolations(ex.getBeanResults().stream().flatMap(errors -> errors.getFieldErrors().stream()).toList());
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        return respond(HttpStatus.BAD_REQUEST, PARAMS_ERROR, List.of(new ErrorDetail(ex.getPropertyName(),
                "Некорректное значение '" + ex.getValue() + "'")));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        if (ex.getCause() instanceof InvalidFormatException invalid && invalid.getTargetType().isEnum()) {
            return respond(HttpStatus.UNPROCESSABLE_ENTITY, CONSTRAINT_ERROR, List.of(new ErrorDetail(path(invalid),
                    "Недопустимое значение '" + invalid.getValue() + "'. Допустимые: "
                            + Arrays.stream(invalid.getTargetType().getEnumConstants()).map(Object::toString)
                            .collect(Collectors.joining(", ")))));
        }
        if (ex.getCause() instanceof UnrecognizedPropertyException unrecognized) {
            return respond(HttpStatus.BAD_REQUEST, FORMAT_ERROR, List.of(new ErrorDetail(path(unrecognized), "Неизвестное поле")));
        }
        if (ex.getCause() instanceof MismatchedInputException mismatched && mismatched.getTargetType() != null) {
            return respond(HttpStatus.BAD_REQUEST, FORMAT_ERROR, List.of(new ErrorDetail(path(mismatched),
                    "Ожидалось значение типа " + mismatched.getTargetType().getSimpleName())));
        }
        return respond(HttpStatus.BAD_REQUEST, FORMAT_ERROR, List.of(new ErrorDetail("body",
                "Тело запроса отсутствует или не является корректным JSON")));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                     HttpHeaders headers, HttpStatusCode status,
                                                                     WebRequest request) {
        return respond(HttpStatus.BAD_REQUEST, FORMAT_ERROR, List.of(new ErrorDetail("Content-Type",
                "Ожидается application/json, получено '" + ex.getContentType() + "'")));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        return ResponseEntity.status(status).headers(headers)
                .body(new ErrorResponse(status.getReasonPhrase(), List.of()));
    }

    private static ResponseEntity<Object> bodyViolations(List<FieldError> errors) {
        List<ErrorDetail> details = errors.stream()
                .map(error -> new ErrorDetail(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(ErrorDetail::field))
                .toList();
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
