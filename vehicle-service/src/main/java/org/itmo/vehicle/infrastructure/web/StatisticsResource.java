package org.itmo.vehicle.infrastructure.web;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.itmo.vehicle.application.VehicleService;
import org.itmo.vehicle.infrastructure.web.generated.api.StatisticsApi;

@Dependent
@Path("/vehicles/statistics")
public class StatisticsResource implements StatisticsApi {

    private final VehicleService service;
    private final VehicleDtoMapper mapper;

    @Inject
    public StatisticsResource(VehicleService service, VehicleDtoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    public Response getEnginePowerSum() {
        return json(mapper.toDto(service.enginePowerStatistics()));
    }

    @Override
    public Response getNumberOfWheelsAverage() {
        return json(mapper.toDto(service.wheelsStatistics()));
    }

    @Override
    public Response getVehicleWithMaxName() {
        return json(mapper.toDto(service.vehicleWithMaxName()));
    }

    private static Response json(Object entity) {
        return Response.ok(entity, MediaType.APPLICATION_JSON_TYPE).build();
    }
}
