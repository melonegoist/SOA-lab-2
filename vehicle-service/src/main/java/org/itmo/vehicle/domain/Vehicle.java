package org.itmo.vehicle.domain;

import java.time.ZonedDateTime;
import java.util.Objects;

public final class Vehicle {

    private final int id;
    private final ZonedDateTime creationDate;
    private final VehicleDetails details;

    public Vehicle(int id, ZonedDateTime creationDate, VehicleDetails details) {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive: " + id);
        }
        this.id = id;
        this.creationDate = Objects.requireNonNull(creationDate, "creationDate");
        this.details = Objects.requireNonNull(details, "details");
    }

    /**
     * Full replacement (PUT): the identity and the creation date stay.
     */
    public Vehicle withDetails(VehicleDetails newDetails) {
        return new Vehicle(id, creationDate, newDetails);
    }

    public int getId() {
        return id;
    }

    public ZonedDateTime getCreationDate() {
        return creationDate;
    }

    public VehicleDetails getDetails() {
        return details;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Vehicle vehicle && vehicle.id == id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "Vehicle[id=" + id + ", creationDate=" + creationDate + ", " + details + "]";
    }
}
