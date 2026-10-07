package ru.itmo.soa.workers.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.itmo.soa.workers.config.WebConfig;
import ru.itmo.soa.workers.domain.Coordinates;
import ru.itmo.soa.workers.domain.Person;
import ru.itmo.soa.workers.domain.Position;
import ru.itmo.soa.workers.domain.Worker;
import ru.itmo.soa.workers.dto.AverageSalary;
import ru.itmo.soa.workers.dto.WorkerPage;
import ru.itmo.soa.workers.error.ApiException;
import ru.itmo.soa.workers.query.FilterCondition;
import ru.itmo.soa.workers.query.FilterOperation;
import ru.itmo.soa.workers.query.WorkerField;
import ru.itmo.soa.workers.service.WorkerService;

@SpringJUnitWebConfig(classes = {WebConfig.class, WorkerControllerTest.Mocks.class})
class WorkerControllerTest {

    private static final String VALID_BODY = """
            {
              "name": "Ivan Petrov",
              "coordinates": {"x": 12, "y": -45.7},
              "salary": 85000,
              "startDate": "2024-01-15",
              "endDate": null,
              "position": "DEVELOPER",
              "person": {
                "passportID": "AB1234567",
                "location": {"x": 10.5, "y": 20.25, "z": 300, "name": "Saint Petersburg"}
              }
            }
            """;

    @Configuration
    static class Mocks {
        @Bean
        WorkerService workerService() {
            return mock(WorkerService.class);
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private WorkerService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(service);
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private static Worker worker() {
        Worker worker = new Worker();
        worker.setId(1);
        worker.setName("Ivan Petrov");
        Coordinates coordinates = new Coordinates();
        coordinates.setX(12);
        coordinates.setY(-45.7);
        worker.setCoordinates(coordinates);
        worker.setSalary(85000L);
        worker.setStartDate(LocalDate.of(2024, 1, 15));
        worker.setPosition(Position.DEVELOPER);
        worker.setPerson(new Person());
        return worker;
    }

    @Test
    void createsWorkerWithLocationHeader() throws Exception {
        when(service.create(any(Worker.class))).thenReturn(worker());

        mvc.perform(post("/v1/workers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/v1/workers/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.startDate").value("2024-01-15"))
                .andExpect(jsonPath("$.endDate").value(nullValue()))
                .andExpect(jsonPath("$.person.passportID").value(nullValue()))
                .andExpect(jsonPath("$.person.location").value(nullValue()));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "85000|\"85000\"|salary",
            "\"x\": 12|\"x\": 12.5|coordinates.x",
            "2024-01-15|15.01.2024|startDate",
            "\"name\": \"Ivan Petrov\",|\"name\": \"Ivan Petrov\", \"extra\": 1,|extra",
            "\"salary\": 85000,|''|salary",
            "\"coordinates\": {\"x\": 12, \"y\": -45.7},|\"coordinates\": {\"y\": -45.7},|coordinates.x",
            "\"z\": 300,|''|person.location.z"
    })
    void rejectsMalformedOrMissingFieldsWith400(String from, String to, String field) throws Exception {
        mvc.perform(post("/v1/workers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY.replace(from, to)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Некорректный формат тела запроса"))
                .andExpect(jsonPath("$.details[0].field").value(field));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "\"Ivan Petrov\"|\"  \"|name",
            "85000|0|salary",
            "AB1234567|AB12|person.passportID",
            "Saint Petersburg|''|person.location.name",
            "DEVELOPER|MANAGER|position"
    })
    void rejectsConstraintViolationsWith422(String from, String to, String field) throws Exception {
        mvc.perform(post("/v1/workers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY.replace(from, to)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Нарушены ограничения полей"))
                .andExpect(jsonPath("$.details[0].field").value(field));
    }

    @Test
    void rejectsMalformedJsonWith400() throws Exception {
        mvc.perform(post("/v1/workers").contentType(MediaType.APPLICATION_JSON).content("{\"name\": "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsNonJsonContentTypeWith400() throws Exception {
        mvc.perform(post("/v1/workers").contentType(MediaType.TEXT_PLAIN).content(VALID_BODY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mapsServiceConflictTo409() throws Exception {
        when(service.create(any(Worker.class)))
                .thenThrow(ApiException.conflict("Работник с passportID 'AB1234567' уже существует"));

        mvc.perform(post("/v1/workers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void returnsWorkerById() throws Exception {
        when(service.get(1)).thenReturn(worker());

        mvc.perform(get("/v1/workers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ivan Petrov"));
    }

    @ParameterizedTest
    @CsvSource({"abc", "0", "-5"})
    void rejectsInvalidIdWith400(String id) throws Exception {
        mvc.perform(get("/v1/workers/" + id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("id"));
    }

    @Test
    void mapsNotFoundTo404() throws Exception {
        when(service.get(999)).thenThrow(ApiException.notFound("Работник с id=999 не найден"));

        mvc.perform(get("/v1/workers/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Работник с id=999 не найден"));
    }

    @Test
    void updatesWorker() throws Exception {
        when(service.update(eq(1), any(Worker.class))).thenReturn(worker());

        mvc.perform(put("/v1/workers/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidBodyOnUpdate() throws Exception {
        mvc.perform(put("/v1/workers/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY.replace("85000", "-1")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deletesWorkerWith204() throws Exception {
        mvc.perform(delete("/v1/workers/1")).andExpect(status().isNoContent());

        verify(service).delete(1);
    }

    @Test
    void listsWorkersWithParsedQuery() throws Exception {
        when(service.list(anyList(), any(Sort.class), anyInt(), anyInt()))
                .thenReturn(new WorkerPage(List.of(worker()), 1, 5, 6, 2));

        mvc.perform(get("/v1/workers")
                        .param("sort", "-salary,name")
                        .param("filter", "salary[gte]=1000", "name[substr]=a,b")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));

        verify(service).list(List.of(
                new FilterCondition(WorkerField.SALARY,
                        FilterOperation.GTE, 1000L),
                new FilterCondition(WorkerField.NAME,
                        FilterOperation.SUBSTR, "a,b")),
                Sort.by(Sort.Order.desc("salary"), Sort.Order.asc("name"), Sort.Order.asc("id")), 1, 5);
    }

    @ParameterizedTest
    @CsvSource({"page,-1", "size,0", "size,101", "page,x", "page,107374183"})
    void rejectsInvalidPaginationWith400(String param, String value) throws Exception {
        mvc.perform(get("/v1/workers").param(param, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value(param));
    }

    @Test
    void rejectsUnsupportedFilterOperationWith422() throws Exception {
        mvc.perform(get("/v1/workers").param("filter", "salary[xyz]=1"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void returnsAverageSalaryForEmptyCollection() throws Exception {
        when(service.averageSalary()).thenReturn(new AverageSalary(null, 0));

        mvc.perform(get("/v1/workers/salary/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.average").value(nullValue()))
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void deletesOneBySalary() throws Exception {
        when(service.deleteOneBySalary(85000L)).thenReturn(worker());

        mvc.perform(delete("/v1/workers/salary/value/85000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salary").value(85000));
    }

    @ParameterizedTest
    @CsvSource({"0", "-1", "abc"})
    void rejectsInvalidSalaryWith400(String salary) throws Exception {
        mvc.perform(delete("/v1/workers/salary/value/" + salary))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("salary"));
    }

    @Test
    void searchesByName() throws Exception {
        when(service.searchByName("Iv", 0, 20)).thenReturn(new WorkerPage(List.of(worker()), 0, 20, 1, 1));

        mvc.perform(get("/v1/workers/search/name/Iv"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Ivan Petrov"));
    }

    @Test
    void rejectsSearchPageBeyondAddressableOffsetWith400() throws Exception {
        mvc.perform(get("/v1/workers/search/name/Iv").param("page", "21474837").param("size", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("page"));
    }

    @Test
    void rejectsEmptySearchSubstringWith400() throws Exception {
        mvc.perform(get("/v1/workers/search/name/"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("substring"));
    }
}
