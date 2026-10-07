package ru.itmo.soa.workers.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class Coordinates {

    @NotNull(message = Worker.REQUIRED)
    @Column(name = "coordinates_x", nullable = false)
    private Integer x;

    @NotNull(message = Worker.REQUIRED)
    @Column(name = "coordinates_y", nullable = false)
    private Double y;
}
