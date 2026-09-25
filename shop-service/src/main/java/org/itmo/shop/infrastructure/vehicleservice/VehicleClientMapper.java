package org.itmo.shop.infrastructure.vehicleservice;

import org.itmo.shop.domain.*;
import org.itmo.shop.infrastructure.vehicleservice.generated.model.*;

final class VehicleClientMapper {

    private VehicleClientMapper() {
    }

    static Vehicle toDomain(VehicleDto dto) {
        CoordinatesDto coordinates = required(dto.getCoordinates(), "coordinates");
        return new Vehicle(
                required(dto.getId(), "id"),
                required(dto.getName(), "name"),
                new Coordinates(required(coordinates.getX(), "coordinates.x"), required(coordinates.getY(), "coordinates.y")),
                required(dto.getCreationDate(), "creationDate"),
                required(dto.getEnginePower(), "enginePower"),
                dto.getNumberOfWheels(),
                dto.getMileage(),
                dto.getType() == null ? null : VehicleType.valueOf(dto.getType().getValue()),
                dto.getFuelType() == null ? null : FuelType.valueOf(dto.getFuelType().getValue()));
    }

    static VehicleUpdateRequestDto toUpdateRequest(Vehicle vehicle) {
        return new VehicleUpdateRequestDto()
                .name(vehicle.name())
                .coordinates(new CoordinatesDto().x(vehicle.coordinates().x()).y(vehicle.coordinates().y()))
                .enginePower(vehicle.enginePower())
                .numberOfWheels(vehicle.numberOfWheels())
                .mileage(vehicle.mileage())
                .type(vehicle.type() == null ? null : NullableVehicleTypeDto.fromValue(vehicle.type().name()))
                .fuelType(vehicle.fuelType() == null ? null : NullableFuelTypeDto.fromValue(vehicle.fuelType().name()));
    }

    private static <T> T required(T value, String field) {
        if (value == null) {
            throw new VehicleCatalogException(
                    "The vehicle service returned a vehicle without " + field + ".", null);
        }
        return value;
    }
}
