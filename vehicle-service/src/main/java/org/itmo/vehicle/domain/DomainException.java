package org.itmo.vehicle.domain;

public abstract sealed class DomainException extends RuntimeException
        permits InvariantViolationException, VehicleNotFoundException, NoVehiclesException {

    protected DomainException(String message) {
        super(message);
    }
}
