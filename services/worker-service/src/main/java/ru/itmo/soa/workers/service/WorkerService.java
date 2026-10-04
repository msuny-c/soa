package ru.itmo.soa.workers.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.soa.workers.domain.Worker;
import ru.itmo.soa.workers.dto.AverageSalary;
import ru.itmo.soa.workers.dto.WorkerPage;
import ru.itmo.soa.workers.error.ApiException;
import ru.itmo.soa.workers.query.FilterCondition;
import ru.itmo.soa.workers.query.WorkerSpecifications;
import ru.itmo.soa.workers.repository.WorkerRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class WorkerService {

    static final String PASSPORT_ID_KEY = "soa_workers_passport_id_key";

    private final WorkerRepository repository;

    @Transactional(readOnly = true)
    public WorkerPage list(List<FilterCondition> filters, Sort sort, int page, int size) {
        return WorkerPage.of(repository.findAll(WorkerSpecifications.matching(filters), PageRequest.of(page, size, sort)));
    }

    @Transactional(readOnly = true)
    public Worker get(int id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Работник с id=" + id + " не найден"));
    }

    public Worker create(Worker worker) {
        return save(worker);
    }

    public Worker update(int id, Worker worker) {
        Worker existing = get(id);
        worker.setId(existing.getId());
        worker.setCreationDate(existing.getCreationDate());
        return save(worker);
    }

    public void delete(int id) {
        repository.delete(get(id));
    }

    @Transactional(readOnly = true)
    public AverageSalary averageSalary() {
        return new AverageSalary(repository.averageSalary(), repository.count());
    }

    public Worker deleteOneBySalary(long salary) {
        Worker worker = repository.findFirstBySalaryOrderByIdAsc(salary)
                .orElseThrow(() -> ApiException.notFound("Объект с salary=" + salary + " не найден в коллекции"));
        repository.delete(worker);
        return worker;
    }

    @Transactional(readOnly = true)
    public WorkerPage searchByName(String substring, int page, int size) {
        return WorkerPage.of(repository.findAll(WorkerSpecifications.nameContains(substring),
                PageRequest.of(page, size, Sort.by("id"))));
    }

    private Worker save(Worker worker) {
        try {
            return repository.saveAndFlush(worker);
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException violation
                    && PASSPORT_ID_KEY.equals(violation.getConstraintName())) {
                throw ApiException.conflict("Работник с passportID '" + worker.getPerson().getPassportID() + "' уже существует");
            }
            throw ApiException.unprocessable("Нарушены ограничения полей", "body",
                    "Значения полей не прошли проверку ограничений хранилища");
        }
    }
}
