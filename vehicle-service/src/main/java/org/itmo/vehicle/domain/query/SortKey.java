package org.itmo.vehicle.domain.query;

import java.util.Objects;

public record SortKey(VehicleField field, Direction direction) {

    public enum Direction {
        ASCENDING,
        DESCENDING
    }

    public SortKey {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(direction, "direction");
    }

    public static SortKey ascending(VehicleField field) {
        return new SortKey(field, Direction.ASCENDING);
    }

    public static SortKey descending(VehicleField field) {
        return new SortKey(field, Direction.DESCENDING);
    }
}
