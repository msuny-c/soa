package ru.itmo.soa.workers.dto;

import java.util.List;
import org.springframework.data.domain.Page;
import ru.itmo.soa.workers.domain.Worker;

public record WorkerPage(List<Worker> items, int page, int size, long totalElements, int totalPages) {

    public static WorkerPage of(Page<Worker> page) {
        return new WorkerPage(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
