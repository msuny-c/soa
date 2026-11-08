package ru.itmo.soa.workers.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS mapping ported verbatim from the lab 1/2 WebConfig so direct (non-gateway)
 * API consumers keep working.
 */
@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // Spring 5.2 has no allowedOriginPatterns; no credentials are used, "*" is equivalent here.
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .exposedHeaders(HttpHeaders.LOCATION);
    }
}
