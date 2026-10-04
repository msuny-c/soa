package ru.itmo.soa.workers.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import ru.itmo.soa.workers.domain.Position;
import ru.itmo.soa.workers.error.ApiException;

class WorkerQueryParserTest {

    @Test
    void sortsByIdWhenNothingGiven() {
        assertEquals(Sort.by("id"), WorkerQueryParser.parseSort(null));
        assertEquals(List.of(), WorkerQueryParser.parseFilters(null));
    }

    @Test
    void parsesMultiFieldSortWithDirections() {
        assertEquals(Sort.by(Sort.Order.desc("salary"), Sort.Order.asc("name"),
                        Sort.Order.asc("person.location.name"), Sort.Order.asc("id")),
                WorkerQueryParser.parseSort(List.of("-salary,name", "person.location.name")));
    }

    @Test
    void doesNotAppendIdWhenAlreadySorted() {
        assertEquals(Sort.by(Sort.Order.desc("id")), WorkerQueryParser.parseSort(List.of("-id")));
    }

    @ParameterizedTest
    @EnumSource(WorkerField.class)
    void acceptsSortByEveryField(WorkerField field) {
        Sort sort = WorkerQueryParser.parseSort(List.of("-" + field.getApiName()));

        assertEquals(Sort.Direction.DESC, sort.getOrderFor(field.getApiName()).getDirection());
    }

    @ParameterizedTest
    @ValueSource(strings = {"foo", "salary,,name", "-", "person.location"})
    void rejectsInvalidSortWith400(String sort) {
        ApiException ex = assertThrows(ApiException.class, () -> WorkerQueryParser.parseSort(List.of(sort)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void parsesTypedFilterValues() {
        assertEquals(List.of(
                new FilterCondition(WorkerField.SALARY, FilterOperation.GTE, 1000L),
                new FilterCondition(WorkerField.COORDINATES_Y, FilterOperation.LT, -4.5),
                new FilterCondition(WorkerField.PERSON_LOCATION_X, FilterOperation.EQ, 10.5f),
                new FilterCondition(WorkerField.START_DATE, FilterOperation.GT, LocalDate.of(2024, 1, 15)),
                new FilterCondition(WorkerField.CREATION_DATE, FilterOperation.LTE, Instant.parse("2026-09-07T12:30:00Z")),
                new FilterCondition(WorkerField.POSITION, FilterOperation.NE, Position.BAKER),
                new FilterCondition(WorkerField.NAME, FilterOperation.SUBSTR, "Iv"),
                new FilterCondition(WorkerField.END_DATE, FilterOperation.NULL, true),
                new FilterCondition(WorkerField.PERSON_LOCATION_NAME, FilterOperation.EQ, "a=b")
        ), WorkerQueryParser.parseFilters(List.of(
                "salary[gte]=1000",
                "coordinates.y[lt]=-4.5",
                "person.location.x[eq]=10.5",
                "startDate[gt]=2024-01-15",
                "creationDate[lte]=2026-09-07T12:30:00Z",
                "position[ne]=BAKER",
                "name[substr]=Iv",
                "endDate[null]=true",
                "person.location.name[eq]=a=b")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"salary=1000", "salary[gte]", "foo[eq]=1", "[eq]=1"})
    void rejectsMalformedFilterOrUnknownFieldWith400(String filter) {
        ApiException ex = assertThrows(ApiException.class, () -> WorkerQueryParser.parseFilters(List.of(filter)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "salary[xyz]=1",
            "salary[substr]=1",
            "position[gt]=BAKER",
            "salary[eq]=abc",
            "salary[eq]=",
            "salary[eq]=1.5",
            "id[eq]=99999999999",
            "startDate[eq]=15.01.2024",
            "position[eq]=MANAGER",
            "endDate[null]=yes"
    })
    void rejectsUnsupportedOperationOrValueWith422(String filter) {
        ApiException ex = assertThrows(ApiException.class, () -> WorkerQueryParser.parseFilters(List.of(filter)));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
    }
}
