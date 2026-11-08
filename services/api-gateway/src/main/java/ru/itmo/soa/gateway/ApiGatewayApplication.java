package ru.itmo.soa.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

/**
 * API Gateway (Zuul Proxy): every external request to the worker service goes
 * through it. Routes are served from the config server; upstream instances are
 * resolved in Eureka and load-balanced with Ribbon (round-robin by default).
 */
@SpringBootApplication
@EnableZuulProxy
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
