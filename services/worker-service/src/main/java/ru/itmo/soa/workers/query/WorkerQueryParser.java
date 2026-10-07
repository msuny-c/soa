package ru.itmo.soa.workers.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Sort;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import ru.itmo.soa.workers.error.ApiException;

@UtilityClass
public class WorkerQueryParser {

    private static final Pattern FILTER_PATTERN = Pattern.compile("^([A-Za-z.]+)\\[([^\\]]*)]=(.*)$", Pattern.DOTALL);
    private static final ObjectMapper VALUE_CONVERTER = Jackson2ObjectMapperBuilder.json().build();

    private static final String SORT_ERROR = "Некорректный параметр сортировки";
    private static final String FILTER_FORMAT_ERROR = "Некорректный параметр фильтрации";
    private static final String FILTER_OPERATION_ERROR = "Недопустимая операция в фильтре";
    private static final String FILTER_VALUE_ERROR = "Недопустимое значение в фильтре";

    public static Sort parseSort(List<String> rawValues) {
        List<Sort.Order> orders = new ArrayList<>();
        for (String raw : nullToEmpty(rawValues)) {
            for (String token : raw.split(",")) {
                String trimmed = token.trim();
                boolean descending = trimmed.startsWith("-");
                String name = descending ? trimmed.substring(1) : trimmed;
                WorkerField field = WorkerField.byApiName(name).orElseThrow(() -> ApiException.badRequest(SORT_ERROR,
                        "sort", "Неизвестное поле '" + name + "'. Допустимые поля: " + WorkerField.allowedNames()));
                orders.add(new Sort.Order(descending ? Sort.Direction.DESC : Sort.Direction.ASC, field.getApiName()));
            }
        }
        Sort sort = Sort.by(orders);
        return sort.getOrderFor(WorkerField.ID.getApiName()) == null ? sort.and(Sort.by(WorkerField.ID.getApiName())) : sort;
    }

    public static List<FilterCondition> parseFilters(List<String> rawValues) {
        return nullToEmpty(rawValues).stream().map(WorkerQueryParser::parseFilter).toList();
    }

    private static FilterCondition parseFilter(String raw) {
        Matcher matcher = FILTER_PATTERN.matcher(raw);
        if (!matcher.matches()) {
            throw ApiException.badRequest(FILTER_FORMAT_ERROR, "filter",
                    "Условие '" + raw + "' должно иметь вид поле[операция]=значение");
        }
        String fieldName = matcher.group(1);
        String operationName = matcher.group(2);
        WorkerField field = WorkerField.byApiName(fieldName).orElseThrow(() -> ApiException.badRequest(
                FILTER_FORMAT_ERROR, "filter",
                "Неизвестное поле '" + fieldName + "'. Допустимые поля: " + WorkerField.allowedNames()));
        FilterOperation operation = FilterOperation.byApiName(operationName)
                .filter(op -> op.supports(field.getType()))
                .orElseThrow(() -> ApiException.unprocessable(FILTER_OPERATION_ERROR, "filter",
                        "Операция '" + operationName + "' не поддерживается для поля '" + fieldName
                                + "'. Допустимые: " + FilterOperation.allowedNames()));
        Class<?> valueType = operation == FilterOperation.NULL ? Boolean.class : field.getType();
        return new FilterCondition(field, operation, convert(matcher.group(3), valueType, fieldName));
    }

    private static Object convert(String raw, Class<?> type, String fieldName) {
        try {
            Object value = VALUE_CONVERTER.convertValue(raw, type);
            if (value != null) {
                return value;
            }
        } catch (IllegalArgumentException ignored) {
        }
        throw ApiException.unprocessable(FILTER_VALUE_ERROR, "filter",
                "Значение '" + raw + "' для поля '" + fieldName + "' должно иметь тип " + type.getSimpleName());
    }

    private static List<String> nullToEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }
}
