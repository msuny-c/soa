package ru.itmo.soa.workers.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import ru.itmo.soa.workers.domain.Worker;

public interface WorkerRepository extends JpaRepository<Worker, Integer>, JpaSpecificationExecutor<Worker> {

    Optional<Worker> findFirstBySalaryOrderByIdAsc(Long salary);

    @Query("select avg(w.salary) from Worker w")
    Double averageSalary();
}
