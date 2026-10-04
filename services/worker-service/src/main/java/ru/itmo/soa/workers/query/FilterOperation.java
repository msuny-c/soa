package ru.itmo.soa.workers.query;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FilterOperation {
    EQ("eq"),
    NE("ne"),
    GT("gt"),
    GTE("gte"),
    LT("lt"),
    LTE("lte"),
    SUBSTR("substr"),
    NULL("null");

    private final String apiName;

    public boolean supports(Class<?> type) {
        return switch (this) {
            case EQ, NE, NULL -> true;
            case GT, GTE, LT, LTE -> !type.isEnum();
            case SUBSTR -> type == String.class;
        };
    }

    public static Optional<FilterOperation> byApiName(String apiName) {
        return Arrays.stream(values()).filter(op -> op.apiName.equals(apiName)).findFirst();
    }

    public static String allowedNames() {
        return Arrays.stream(values()).map(FilterOperation::getApiName).collect(Collectors.joining(", "));
    }
}
