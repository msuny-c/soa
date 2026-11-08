package ru.itmo.soa.workers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the worker service as a Spring Cloud application.
 * Configuration (datasource, servlet context, eureka instance metadata) is fetched
 * from the config server; the instance registers itself in Eureka and is load-balanced
 * by Ribbon behind the Zuul API gateway.
 */
@SpringBootApplication
public class WorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }
}
