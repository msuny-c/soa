package ru.itmo.soa.hr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.itmo.soa.hr.client.WorkerServiceClient;
import ru.itmo.soa.hr.dto.IndexationResult;
import ru.itmo.soa.hr.dto.OrganizationIndexationResult;
import ru.itmo.soa.hr.error.ApiException;

class IndexationServiceTest {

    private final FakeClient client = new FakeClient();
    private final IndexationService service = new IndexationService(client);

    @Test
    void indexesSingleWorkerAndSendsFullWriteObject() {
        client.add(worker(1, 85000, "Acme"));

        IndexationResult result = service.indexWorker(1, new BigDecimal("1.1"));

        assertEquals(new IndexationResult(1, 85000, 93500), result);
        JsonObject sent = client.updates.get(1);
        assertEquals(93500, sent.getJsonNumber("salary").longValueExact());
        assertFalse(sent.containsKey("id"));
        assertFalse(sent.containsKey("creationDate"));
        assertEquals("Ivan", sent.getString("name"));
        assertTrue(sent.isNull("endDate"));
        assertEquals("Acme", sent.getJsonObject("person").getJsonObject("location").getString("name"));
    }

    @Test
    void propagatesNotFound() {
        ApiException ex = assertThrows(ApiException.class, () -> service.indexWorker(42, BigDecimal.TEN));

        assertEquals(404, ex.getStatus());
    }

    @Test
    void rejectsOverflowWith422WithoutUpdate() {
        client.add(worker(1, Long.MAX_VALUE, "Acme"));

        ApiException ex = assertThrows(ApiException.class, () -> service.indexWorker(1, BigDecimal.TEN));

        assertEquals(422, ex.getStatus());
        assertEquals("newSalary", ex.getDetails().get(0).field());
        assertTrue(client.updates.isEmpty());
    }

    @Test
    void indexesAllWorkersOfOrganizationAcrossPages() {
        for (int id = 1; id <= 150; id++) {
            client.add(worker(id, 1000, id % 3 == 0 ? "Other" : "Acme"));
        }
        client.add(worker(151, 2000, "Acme"));

        OrganizationIndexationResult result = service.indexOrganization("Acme", new BigDecimal("1.5"));

        assertEquals(101, result.updatedCount());
        assertEquals(new IndexationResult(151, 2000, 3000), result.results().get(100));
        assertEquals(101, client.updates.size());
        assertEquals(1500, client.updates.get(2).getJsonNumber("salary").longValueExact());
    }

    @Test
    void encodesOrganizationSoThatUpstreamDecodesItUnchanged() {
        client.add(worker(1, 1000, "50%41 {x}+y&z=1"));
        client.add(worker(2, 1000, "50A {x} y&z=1"));

        OrganizationIndexationResult result = service.indexOrganization("50%41 {x}+y&z=1", new BigDecimal("2"));

        assertEquals(List.of(new IndexationResult(1, 1000, 2000)), result.results());
        assertEquals("person.location.name%5Beq%5D%3D50%2541%20%7Bx%7D%2By%26z%3D1",
                IndexationService.equalsFilter("person.location.name", "50%41 {x}+y&z=1"));
    }

    @Test
    void returns404ForUnknownOrganization() {
        client.add(worker(1, 1000, "Acme"));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.indexOrganization("Unknown Corp", BigDecimal.TEN));

        assertEquals(404, ex.getStatus());
    }

    @Test
    void doesNotUpdateAnyoneWhenOneResultIsInvalid() {
        client.add(worker(1, 1000, "Acme"));
        client.add(worker(7, Long.MAX_VALUE / 2, "Acme"));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.indexOrganization("Acme", new BigDecimal("3")));

        assertEquals(422, ex.getStatus());
        assertTrue(ex.getDetails().get(0).issue().startsWith("Для работника id=7"));
        assertTrue(client.updates.isEmpty());
    }

    private static JsonObject worker(int id, long salary, String organization) {
        return Json.createObjectBuilder()
                .add("id", id)
                .add("name", "Ivan")
                .add("coordinates", Json.createObjectBuilder().add("x", 1).add("y", 2.5))
                .add("creationDate", "2026-09-07T12:30:00Z")
                .add("salary", salary)
                .add("startDate", "2024-01-15")
                .addNull("endDate")
                .add("position", "DEVELOPER")
                .add("person", Json.createObjectBuilder()
                        .addNull("passportID")
                        .add("location", Json.createObjectBuilder()
                                .add("x", 1.5).add("y", 2.5).add("z", 3).add("name", organization)))
                .build();
    }

    private static final class FakeClient implements WorkerServiceClient {

        private final Map<Integer, JsonObject> workers = new LinkedHashMap<>();
        private final Map<Integer, JsonObject> updates = new LinkedHashMap<>();

        void add(JsonObject worker) {
            workers.put(worker.getInt("id"), worker);
        }

        @Override
        public JsonObject getWorker(int id) {
            JsonObject worker = workers.get(id);
            if (worker == null) {
                throw new ApiException(404, "Работник с id=" + id + " не найден в Worker Collection Service", List.of());
            }
            return worker;
        }

        @Override
        public JsonObject findWorkers(String filter, int page, int size) {
            String organization = URLDecoder.decode(filter, StandardCharsets.UTF_8)
                    .substring("person.location.name[eq]=".length());
            List<JsonObject> matching = new ArrayList<>();
            for (JsonObject worker : workers.values()) {
                if (organization.equals(worker.getJsonObject("person").getJsonObject("location").getString("name"))) {
                    matching.add(worker);
                }
            }
            JsonArrayBuilder items = Json.createArrayBuilder();
            matching.stream().skip((long) page * size).limit(size).forEach(items::add);
            int totalPages = (matching.size() + size - 1) / size;
            return Json.createObjectBuilder()
                    .add("items", items)
                    .add("page", page)
                    .add("size", size)
                    .add("totalElements", matching.size())
                    .add("totalPages", totalPages)
                    .build();
        }

        @Override
        public JsonObject updateWorker(int id, JsonObject workerWrite) {
            updates.put(id, workerWrite);
            return workerWrite;
        }
    }
}
