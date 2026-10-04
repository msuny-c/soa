package ru.itmo.soa.workers.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class Person {

    @Size(min = 7, max = 23, message = "Длина строки должна быть от {min} до {max} символов")
    @Column(name = "passport_id", unique = true)
    private String passportID;

    @Valid
    @Embedded
    private Location location;
}
