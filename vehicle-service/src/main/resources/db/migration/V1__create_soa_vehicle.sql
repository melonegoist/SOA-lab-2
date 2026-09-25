-- The vehicle collection. The integrity constraints of the model are repeated
-- here as CHECK constraints: the database is the last line of defence, even
-- for data that does not come through the service.
--
-- On helios a student may not create schemas and shares one personal schema
-- between all courses, hence the soa_ prefix: a namespace inside that schema.

CREATE TABLE soa_vehicle (
    -- int4, as the contract's int32; GENERATED ALWAYS forbids choosing an id
    id               integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name             text             NOT NULL,
    coordinate_x     integer          NOT NULL,
    coordinate_y     integer          NOT NULL,
    creation_date    timestamptz      NOT NULL,
    -- a float in the model; double precision so that SUM() is computed in double
    engine_power     double precision NOT NULL,
    number_of_wheels integer,
    mileage          double precision,
    type             varchar(16),
    fuel_type        varchar(16),

    CONSTRAINT soa_vehicle_name_not_blank
        CHECK (name ~ '[^[:space:]]'),
    CONSTRAINT soa_vehicle_coordinate_x_max
        CHECK (coordinate_x <= 471),
    -- positive and within the float range (rejects Infinity and NaN as well)
    CONSTRAINT soa_vehicle_engine_power_positive
        CHECK (engine_power > 0 AND engine_power <= 3.4028234663852886e38),
    CONSTRAINT soa_vehicle_number_of_wheels_positive
        CHECK (number_of_wheels > 0),
    CONSTRAINT soa_vehicle_mileage_not_negative
        CHECK (mileage >= 0 AND mileage < 'Infinity'::double precision),
    CONSTRAINT soa_vehicle_type_known
        CHECK (type IN ('HELICOPTER', 'MOTORCYCLE', 'CHOPPER', 'SPACESHIP')),
    CONSTRAINT soa_vehicle_fuel_type_known
        CHECK (fuel_type IN ('GASOLINE', 'ELECTRICITY', 'MANPOWER', 'ANTIMATTER'))
);
