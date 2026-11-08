package ru.itmo.soa.workers.dto;

import lombok.Value;

@Value
public class AverageSalary {
    Double average;
    long count;
}
