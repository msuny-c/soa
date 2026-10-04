package ru.itmo.soa.workers.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.itmo.soa.workers.domain.Worker;
import ru.itmo.soa.workers.dto.AverageSalary;
import ru.itmo.soa.workers.dto.WorkerPage;
import ru.itmo.soa.workers.error.ApiException;
import ru.itmo.soa.workers.query.WorkerQueryParser;
import ru.itmo.soa.workers.service.WorkerService;

@RestController
@RequestMapping(path = "/v1/workers", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class WorkerController {

    private static final String ID_ISSUE = "Значение должно быть целым числом >= 1";
    private static final String PAGE_ISSUE = "Значение должно быть целым числом >= 0";
    private static final String SIZE_ISSUE = "Значение должно быть целым числом от 1 до 100";

    private final WorkerService service;

    @GetMapping
    public WorkerPage list(@RequestParam MultiValueMap<String, String> params,
                           @RequestParam(defaultValue = "0") @Min(value = 0, message = PAGE_ISSUE) int page,
                           @RequestParam(defaultValue = "20") @Min(value = 1, message = SIZE_ISSUE)
                           @Max(value = 100, message = SIZE_ISSUE) int size) {
        requireAddressableOffset(page, size);
        return service.list(WorkerQueryParser.parseFilters(params.get("filter")),
                WorkerQueryParser.parseSort(params.get("sort")), page, size);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Worker> create(@Valid @RequestBody Worker worker) {
        Worker created = service.create(worker);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}")
                .buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public Worker get(@PathVariable @Min(value = 1, message = ID_ISSUE) int id) {
        return service.get(id);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Worker update(@PathVariable @Min(value = 1, message = ID_ISSUE) int id, @Valid @RequestBody Worker worker) {
        return service.update(id, worker);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Min(value = 1, message = ID_ISSUE) int id) {
        service.delete(id);
    }

    @GetMapping("/salary/average")
    public AverageSalary averageSalary() {
        return service.averageSalary();
    }

    @DeleteMapping("/salary/value/{salary}")
    public Worker deleteOneBySalary(
            @PathVariable @Positive(message = "Значение должно быть целым числом строго больше 0") long salary) {
        return service.deleteOneBySalary(salary);
    }

    @GetMapping("/search/name/{substring}")
    public WorkerPage searchByName(@PathVariable String substring,
                                   @RequestParam(defaultValue = "0") @Min(value = 0, message = PAGE_ISSUE) int page,
                                   @RequestParam(defaultValue = "20") @Min(value = 1, message = SIZE_ISSUE)
                                   @Max(value = 100, message = SIZE_ISSUE) int size) {
        requireAddressableOffset(page, size);
        return service.searchByName(substring, page, size);
    }

    @GetMapping({"/search/name", "/search/name/"})
    public WorkerPage searchByEmptyName() {
        throw ApiException.badRequest("Подстрока поиска не может быть пустой", "substring",
                "Минимальная длина строки — 1 символ");
    }

    private static void requireAddressableOffset(int page, int size) {
        if ((long) page * size > Integer.MAX_VALUE) {
            throw ApiException.badRequest("Некорректные параметры запроса", "page",
                    "Смещение page * size не должно превышать " + Integer.MAX_VALUE + ", получено '" + page + "'");
        }
    }
}
