CREATE TABLE IF NOT EXISTS soa_workers (
    id            SERIAL PRIMARY KEY,
    name          TEXT             NOT NULL CHECK (length(btrim(name)) > 0),
    coordinates_x INTEGER          NOT NULL,
    coordinates_y DOUBLE PRECISION NOT NULL,
    creation_date TIMESTAMPTZ      NOT NULL,
    salary        BIGINT           NOT NULL CHECK (salary > 0),
    start_date    DATE             NOT NULL,
    end_date      DATE,
    position      VARCHAR(32)      NOT NULL CHECK (position IN ('DEVELOPER', 'LEAD_DEVELOPER', 'BAKER')),
    passport_id   VARCHAR(23) CONSTRAINT soa_workers_passport_id_key UNIQUE CHECK (length(passport_id) >= 7),
    location_x    REAL,
    location_y    DOUBLE PRECISION,
    location_z    BIGINT,
    location_name TEXT CHECK (length(btrim(location_name)) > 0),
    CHECK (
        (location_x IS NULL AND location_y IS NULL AND location_z IS NULL AND location_name IS NULL)
        OR (location_x IS NOT NULL AND location_y IS NOT NULL AND location_z IS NOT NULL)
    )
);
