package ru.itmo.soa.workers.domain;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.validation.Valid;
import javax.validation.constraints.Size;
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
