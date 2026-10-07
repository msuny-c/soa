package ru.itmo.soa.workers.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import ru.itmo.soa.workers.domain.Person;
import ru.itmo.soa.workers.domain.Worker;
import ru.itmo.soa.workers.error.ApiException;
import ru.itmo.soa.workers.repository.WorkerRepository;

@ExtendWith(MockitoExtension.class)
class WorkerServiceTest {

    @Mock
    private WorkerRepository repository;

    @InjectMocks
    private WorkerService service;

    @Test
    void mapsPassportUniqueViolationTo409() {
        when(repository.saveAndFlush(any(Worker.class))).thenThrow(new DataIntegrityViolationException("duplicate",
                new ConstraintViolationException("duplicate", new SQLException("duplicate", "23505"),
                        WorkerService.PASSPORT_ID_KEY)));

        ApiException ex = assertThrows(ApiException.class, () -> service.create(worker()));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("Работник с passportID 'AB1234567' уже существует", ex.getMessage());
    }

    @Test
    void mapsOtherIntegrityViolationsTo422() {
        when(repository.saveAndFlush(any(Worker.class))).thenThrow(new DataIntegrityViolationException("invalid",
                new DataException("invalid byte sequence", new SQLException("invalid byte sequence", "22021"))));

        ApiException ex = assertThrows(ApiException.class, () -> service.create(worker()));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        assertEquals("body", ex.getDetails().get(0).field());
    }

    @Test
    void mapsOtherConstraintViolationsTo422() {
        when(repository.saveAndFlush(any(Worker.class))).thenThrow(new DataIntegrityViolationException("check",
                new ConstraintViolationException("check", new SQLException("check", "23514"), "soa_workers_salary_check")));

        ApiException ex = assertThrows(ApiException.class, () -> service.create(worker()));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
    }

    private static Worker worker() {
        Person person = new Person();
        person.setPassportID("AB1234567");
        Worker worker = new Worker();
        worker.setPerson(person);
        return worker;
    }
}
