package org.itmo.vehicle.domain.query;

import java.util.Arrays;
import java.util.Optional;

public enum VehicleField {
    ID("id", ValueType.INTEGER, false),
    NAME("name", ValueType.STRING, false),
    COORDINATES_X("coordinates.x", ValueType.INTEGER, false),
    COORDINATES_Y("coordinates.y", ValueType.INTEGER, false),
    CREATION_DATE("creationDate", ValueType.DATE_TIME, false),
    ENGINE_POWER("enginePower", ValueType.FLOAT, false),
    NUMBER_OF_WHEELS("numberOfWheels", ValueType.INTEGER, true),
    MILEAGE("mileage", ValueType.DOUBLE, true),
    TYPE("type", ValueType.VEHICLE_TYPE, true),
    FUEL_TYPE("fuelType", ValueType.FUEL_TYPE, true);

    private final String path;
    private final ValueType valueType;
    private final boolean nullable;

    VehicleField(String path, ValueType valueType, boolean nullable) {
        this.path = path;
        this.valueType = valueType;
        this.nullable = nullable;
    }

    public static Optional<VehicleField> byPath(String path) {
        return Arrays.stream(values())
                .filter(field -> field.path.equals(path))
                .findFirst();
    }

    public String path() {
        return path;
    }

    public ValueType valueType() {
        return valueType;
    }

    public boolean isNullable() {
        return nullable;
    }
}
