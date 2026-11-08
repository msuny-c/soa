package ru.itmo.soa.workers.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "soa_workers")
@Getter
@Setter
public class Worker {

    static final String REQUIRED = "Поле обязательно и не может быть null";
    static final String NOT_BLANK = "(?s).*\\S.*";
    static final String BLANK = "Строка не может быть пустой";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer id;

    @NotNull(message = REQUIRED)
    @Pattern(regexp = NOT_BLANK, message = BLANK)
    private String name;

    @Valid
    @NotNull(message = REQUIRED)
    @Embedded
    private Coordinates coordinates;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "creation_date", nullable = false, updatable = false)
    private Instant creationDate;

    @NotNull(message = REQUIRED)
    @Positive(message = "Значение должно быть строго больше 0")
    private Long salary;

    @NotNull(message = REQUIRED)
    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @NotNull(message = REQUIRED)
    @Enumerated(EnumType.STRING)
    private Position position;

    @Valid
    @NotNull(message = REQUIRED)
    @Embedded
    private Person person;

    public Person getPerson() {
        return person != null ? person : new Person();
    }

    @PrePersist
    void onCreate() {
        creationDate = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
