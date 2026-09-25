package org.itmo.vehicle.domain;

public record VehicleDetails(
        String name,
        Coordinates coordinates,
        float enginePower,
        Integer numberOfWheels,
        Double mileage,
        VehicleType type,
        FuelType fuelType) {

    public VehicleDetails {
        if (name == null || name.isBlank()) {
            throw new InvariantViolationException("name", "must contain a non-whitespace character");
        }
        if (coordinates == null) {
            throw new InvariantViolationException("coordinates", "must be present");
        }
        // !(x > 0) also rejects NaN
        if (!(enginePower > 0) || Float.isInfinite(enginePower)) {
            throw new InvariantViolationException("enginePower", "must be a finite number greater than zero");
        }
        if (numberOfWheels != null && numberOfWheels <= 0) {
            throw new InvariantViolationException("numberOfWheels", "must be greater than zero");
        }
        if (mileage != null && (!(mileage >= 0) || mileage.isInfinite())) {
            throw new InvariantViolationException("mileage", "must be a finite non-negative number");
        }
    }
}
