package org.itmo.vehicle.infrastructure.web;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.itmo.vehicle.application.VehicleService;
import org.itmo.vehicle.domain.Vehicle;
import org.itmo.vehicle.infrastructure.web.generated.api.VehiclesApi;
import org.itmo.vehicle.infrastructure.web.generated.model.VehicleCreateRequestDto;
import org.itmo.vehicle.infrastructure.web.generated.model.VehicleUpdateRequestDto;

import java.net.URI;
import java.util.List;

@Dependent
@Path("")
public class VehicleResource implements VehiclesApi {

    private final VehicleService service;
    private final VehicleQueryParser queryParser;
    private final VehicleDtoMapper mapper;

    @Inject
    public VehicleResource(VehicleService service, VehicleQueryParser queryParser, VehicleDtoMapper mapper) {
        this.service = service;
        this.queryParser = queryParser;
        this.mapper = mapper;
    }

    @Override
    public Response getVehicles(List<String> filter, List<String> sort, Integer page, Integer size) {
        var query = queryParser.parse(filter, sort, page, size);
        return json(Response.ok(), mapper.toDto(service.find(query)));
    }

    @Override
    public Response createVehicle(VehicleCreateRequestDto request) {
        Vehicle created = service.create(mapper.toDetails(request));
        // A relative Location is resolved against the application's base URI.
        return json(Response.created(URI.create("vehicles/" + created.getId())), mapper.toDto(created));
    }

    @Override
    public Response getVehicleById(Integer id) {
        return json(Response.ok(), mapper.toDto(service.get(id)));
    }

    @Override
    public Response updateVehicle(Integer id, VehicleUpdateRequestDto request) {
        return json(Response.ok(), mapper.toDto(service.replace(id, mapper.toDetails(request))));
    }

    @Override
    public Response deleteVehicle(Integer id) {
        service.delete(id);
        return Response.noContent().build();
    }

    private static Response json(Response.ResponseBuilder builder, Object entity) {
        return builder.type(MediaType.APPLICATION_JSON_TYPE).entity(entity).build();
    }
}
