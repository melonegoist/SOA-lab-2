package org.itmo.vehicle.infrastructure.persistence;

import org.itmo.vehicle.domain.Coordinates;
import org.itmo.vehicle.domain.Vehicle;
import org.itmo.vehicle.domain.VehicleDetails;

import java.time.ZoneId;

final class VehicleEntityMapper {

    private VehicleEntityMapper() {
    }

    static void copyDetails(VehicleDetails details, VehicleEntity entity) {
        entity.setName(details.name());
        entity.setCoordinates(new CoordinatesEmbeddable(details.coordinates().x(), details.coordinates().y()));
        entity.setEnginePower(details.enginePower());
        entity.setNumberOfWheels(details.numberOfWheels());
        entity.setMileage(details.mileage());
        entity.setType(details.type());
        entity.setFuelType(details.fuelType());
    }

    static Vehicle toDomain(VehicleEntity entity, ZoneId zone) {
        VehicleDetails details = new VehicleDetails(
                entity.getName(),
                new Coordinates(entity.getCoordinates().getX(), entity.getCoordinates().getY()),
                (float) entity.getEnginePower(),
                entity.getNumberOfWheels(),
                entity.getMileage(),
                entity.getType(),
                entity.getFuelType());
        return new Vehicle(entity.getId(), entity.getCreationDate().atZoneSameInstant(zone), details);
    }
}
