package org.itmo.shop.infrastructure.web;

import org.itmo.shop.domain.Vehicle;
import org.itmo.shop.domain.VehicleType;
import org.itmo.shop.infrastructure.web.generated.model.*;

final class VehicleDtoMapper {

    private VehicleDtoMapper() {
    }

    static VehicleType toDomain(VehicleTypeDto type) {
        return VehicleType.valueOf(type.getValue());
    }

    static VehicleDto toDto(Vehicle vehicle) {
        return new VehicleDto()
                .id(vehicle.id())
                .name(vehicle.name())
                .coordinates(new CoordinatesDto().x(vehicle.coordinates().x()).y(vehicle.coordinates().y()))
                .creationDate(vehicle.creationDate())
                .enginePower(vehicle.enginePower())
                .numberOfWheels(vehicle.numberOfWheels())
                .mileage(vehicle.mileage())
                .type(vehicle.type() == null ? null : NullableVehicleTypeDto.fromValue(vehicle.type().name()))
                .fuelType(vehicle.fuelType() == null ? null : NullableFuelTypeDto.fromValue(vehicle.fuelType().name()));
    }
}
