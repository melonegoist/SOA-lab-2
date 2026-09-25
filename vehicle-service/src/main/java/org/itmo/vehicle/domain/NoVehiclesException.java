package org.itmo.vehicle.domain;

public final class NoVehiclesException extends DomainException {

    public NoVehiclesException() {
        super("The collection is empty.");
    }
}
