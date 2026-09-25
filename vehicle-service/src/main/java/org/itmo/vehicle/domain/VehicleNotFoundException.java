package org.itmo.vehicle.domain;

public final class VehicleNotFoundException extends DomainException {

    private final int id;

    public VehicleNotFoundException(int id) {
        super("Vehicle with id " + id + " does not exist.");
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
