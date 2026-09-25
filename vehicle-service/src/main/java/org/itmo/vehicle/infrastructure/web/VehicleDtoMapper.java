package org.itmo.vehicle.infrastructure.web;

import jakarta.enterprise.context.ApplicationScoped;
import org.itmo.vehicle.domain.*;
import org.itmo.vehicle.domain.query.Page;
import org.itmo.vehicle.infrastructure.web.generated.model.*;

@ApplicationScoped
public class VehicleDtoMapper {

    public VehicleDetails toDetails(VehicleCreateRequestDto request) {
        return details(request.getName(), request.getCoordinates(), request.getEnginePower(),
                request.getNumberOfWheels(), request.getMileage(), request.getType(), request.getFuelType());
    }

    public VehicleDetails toDetails(VehicleUpdateRequestDto request) {
        return details(request.getName(), request.getCoordinates(), request.getEnginePower(),
                request.getNumberOfWheels(), request.getMileage(), request.getType(), request.getFuelType());
    }

    public VehicleDto toDto(Vehicle vehicle) {
        VehicleDetails details = vehicle.getDetails();
        return new VehicleDto()
                .id(vehicle.getId())
                .name(details.name())
                .coordinates(new CoordinatesDto().x(details.coordinates().x()).y(details.coordinates().y()))
                .creationDate(vehicle.getCreationDate().toOffsetDateTime())
                .enginePower(details.enginePower())
                .numberOfWheels(details.numberOfWheels())
                .mileage(details.mileage())
                .type(details.type() == null ? null : NullableVehicleTypeDto.valueOf(details.type().name()))
                .fuelType(details.fuelType() == null ? null : NullableFuelTypeDto.valueOf(details.fuelType().name()));
    }

    public VehiclePageDto toDto(Page<Vehicle> page) {
        return new VehiclePageDto()
                .items(page.items().stream().map(this::toDto).toList())
                .page(page.page())
                .size(page.size())
                .totalElements(page.totalElements())
                .totalPages(page.totalPages());
    }

    public EnginePowerSumDto toDto(EnginePowerStatistics statistics) {
        return new EnginePowerSumDto()
                .sum(statistics.sum())
                .consideredElements(statistics.consideredElements());
    }

    public NumberOfWheelsAverageDto toDto(WheelsStatistics statistics) {
        return new NumberOfWheelsAverageDto()
                .average(statistics.average())
                .consideredElements(statistics.consideredElements());
    }

    private static VehicleDetails details(String name, CoordinatesDto coordinates, Float enginePower,
                                          Integer numberOfWheels, Double mileage,
                                          NullableVehicleTypeDto type, NullableFuelTypeDto fuelType) {
        return new VehicleDetails(
                name,
                new Coordinates(coordinates.getX(), coordinates.getY()),
                enginePower,
                numberOfWheels,
                mileage,
                type == null ? null : VehicleType.valueOf(type.name()),
                fuelType == null ? null : FuelType.valueOf(fuelType.name()));
    }
}
