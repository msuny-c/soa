package ru.itmo.soa.workers.query;

import jakarta.persistence.criteria.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.itmo.soa.workers.domain.Position;

@Getter
@RequiredArgsConstructor
public enum WorkerField {
    ID("id", Integer.class),
    NAME("name", String.class),
    COORDINATES_X("coordinates.x", Integer.class),
    COORDINATES_Y("coordinates.y", Double.class),
    CREATION_DATE("creationDate", Instant.class),
    SALARY("salary", Long.class),
    START_DATE("startDate", LocalDate.class),
    END_DATE("endDate", LocalDate.class),
    POSITION("position", Position.class),
    PERSON_PASSPORT_ID("person.passportID", String.class),
    PERSON_LOCATION_X("person.location.x", Float.class),
    PERSON_LOCATION_Y("person.location.y", Double.class),
    PERSON_LOCATION_Z("person.location.z", Long.class),
    PERSON_LOCATION_NAME("person.location.name", String.class);

    private final String apiName;
    private final Class<?> type;

    @SuppressWarnings("unchecked")
    public <T> Path<T> path(Path<?> root) {
        Path<?> path = root;
        for (String part : apiName.split("\\.")) {
            path = path.get(part);
        }
        return (Path<T>) path;
    }

    public static Optional<WorkerField> byApiName(String apiName) {
        return Arrays.stream(values()).filter(field -> field.apiName.equals(apiName)).findFirst();
    }

    public static String allowedNames() {
        return Arrays.stream(values()).map(WorkerField::getApiName).collect(Collectors.joining(", "));
    }
}
