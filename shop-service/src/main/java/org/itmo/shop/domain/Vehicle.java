package org.itmo.shop.domain;

import java.time.OffsetDateTime;
import java.util.Objects;

public record Vehicle(int id,
                      String name,
                      Coordinates coordinates,
                      OffsetDateTime creationDate,
                      float enginePower,
                      Integer numberOfWheels,
                      Double mileage,
                      VehicleType type,
                      FuelType fuelType) {

    public Vehicle {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(coordinates, "coordinates");
        Objects.requireNonNull(creationDate, "creationDate");
    }

    public Vehicle withOdometerReset() {
        return new Vehicle(id, name, coordinates, creationDate, enginePower, numberOfWheels, 0.0, type, fuelType);
    }

    public boolean odometerShowsZero() {
        return mileage != null && mileage == 0;
    }
}
