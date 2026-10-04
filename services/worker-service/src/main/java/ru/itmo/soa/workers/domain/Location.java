package ru.itmo.soa.workers.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class Location {

    @NotNull(message = Worker.REQUIRED)
    @Column(name = "location_x")
    private Float x;

    @NotNull(message = Worker.REQUIRED)
    @Column(name = "location_y")
    private Double y;

    @NotNull(message = Worker.REQUIRED)
    @Column(name = "location_z")
    private Long z;

    @Pattern(regexp = Worker.NOT_BLANK, message = Worker.BLANK)
    @Column(name = "location_name")
    private String name;
}
