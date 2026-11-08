package ru.itmo.soa.hr.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import ru.itmo.soa.hr.client.WorkerServiceClient;
import ru.itmo.soa.hr.dto.ErrorDetail;
import ru.itmo.soa.hr.dto.IndexationResult;
import ru.itmo.soa.hr.dto.OrganizationIndexationResult;
import ru.itmo.soa.hr.error.ApiException;

@ApplicationScoped
public class IndexationService {

    static final int PAGE_SIZE = 100;

    private WorkerServiceClient client;

    IndexationService() {
    }

    @Inject
    public IndexationService(@RestClient WorkerServiceClient client) {
        this.client = client;
    }

    public IndexationResult indexWorker(int workerId, BigDecimal coeff) {
        JsonObject worker = client.getWorker(workerId);
        long oldSalary = salaryOf(worker);
        SalaryIndexer.Outcome outcome = SalaryIndexer.index(oldSalary, coeff);
        if (!outcome.isValid()) {
            throw new ApiException(422, outcome.message(), List.of(new ErrorDetail("newSalary", outcome.issue())));
        }
        client.updateWorker(workerId, withSalary(worker, outcome.newSalary()));
        return new IndexationResult(workerId, oldSalary, outcome.newSalary());
    }

    public OrganizationIndexationResult indexOrganization(String organization, BigDecimal coeff) {
        List<JsonObject> workers = findOrganizationWorkers(organization);
        if (workers.isEmpty()) {
            throw new ApiException(404, "Организация '" + organization
                    + "' не найдена. Нет работников с person.location.name='" + organization + "'", List.of());
        }

        List<IndexationResult> planned = new ArrayList<>();
        List<ErrorDetail> violations = new ArrayList<>();
        for (JsonObject worker : workers) {
            int id = worker.getInt("id");
            SalaryIndexer.Outcome outcome = SalaryIndexer.index(salaryOf(worker), coeff);
            if (outcome.isValid()) {
                planned.add(new IndexationResult(id, salaryOf(worker), outcome.newSalary()));
            } else {
                violations.add(new ErrorDetail("newSalary", "Для работника id=" + id + ": " + outcome.issue()));
            }
        }
        if (!violations.isEmpty()) {
            throw new ApiException(422,
                    "Результат индексации для одного из сотрудников недопустим, зарплаты не изменены", violations);
        }

        for (int i = 0; i < planned.size(); i++) {
            try {
                client.updateWorker(planned.get(i).workerID(), withSalary(workers.get(i), planned.get(i).newSalary()));
            } catch (ApiException e) {
                throw e.withMessage(e.getMessage() + ". Индексация прервана: обновлено " + i + " из " + planned.size());
            }
        }
        return new OrganizationIndexationResult(planned);
    }

    private List<JsonObject> findOrganizationWorkers(String organization) {
        List<JsonObject> workers = new ArrayList<>();
        int totalPages = 1;
        for (int page = 0; page < totalPages; page++) {
            JsonObject response = client.findWorkers(equalsFilter("person.location.name", organization), page, PAGE_SIZE);
            workers.addAll(response.getJsonArray("items").getValuesAs(JsonObject.class));
            totalPages = response.getInt("totalPages");
        }
        return workers;
    }

    static String equalsFilter(String field, String value) {
        return URLEncoder.encode(field + "[eq]=" + value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static long salaryOf(JsonObject worker) {
        return worker.getJsonNumber("salary").longValueExact();
    }

    static JsonObject withSalary(JsonObject worker, long newSalary) {
        return Json.createObjectBuilder(worker)
                .remove("id")
                .remove("creationDate")
                .add("salary", newSalary)
                .build();
    }
}
