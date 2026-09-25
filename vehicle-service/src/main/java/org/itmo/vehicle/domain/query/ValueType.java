package org.itmo.vehicle.domain.query;

import org.itmo.vehicle.domain.FuelType;
import org.itmo.vehicle.domain.VehicleType;

import java.time.ZonedDateTime;

public enum ValueType {
    INTEGER(Integer.class, true),
    FLOAT(Float.class, true),
    DOUBLE(Double.class, true),
    DATE_TIME(ZonedDateTime.class, true),
    STRING(String.class, false),
    VEHICLE_TYPE(VehicleType.class, false),
    FUEL_TYPE(FuelType.class, false);

    private final Class<?> javaType;
    private final boolean ordered;

    ValueType(Class<?> javaType, boolean ordered) {
        this.javaType = javaType;
        this.ordered = ordered;
    }

    public Class<?> javaType() {
        return javaType;
    }

    public boolean isOrdered() {
        return ordered;
    }
}
