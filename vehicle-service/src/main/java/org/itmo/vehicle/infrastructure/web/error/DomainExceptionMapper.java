package org.itmo.vehicle.infrastructure.web.error;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.itmo.vehicle.domain.DomainException;
import org.itmo.vehicle.domain.InvariantViolationException;
import org.itmo.vehicle.domain.NoVehiclesException;
import org.itmo.vehicle.domain.VehicleNotFoundException;

import java.util.List;

@Provider
public class DomainExceptionMapper implements ExceptionMapper<DomainException> {

    @Context
    private UriInfo request;

    @Override
    public Response toResponse(DomainException exception) {
        if (exception instanceof InvariantViolationException violation) {
            return Problems.response(400, "The vehicle violates an integrity constraint.", request,
                    List.of(Problems.error(Problems.pointer(violation.getField()), violation.getMessage())));
        }
        if (exception instanceof VehicleNotFoundException || exception instanceof NoVehiclesException) {
            return Problems.response(404, exception.getMessage(), request);
        }
        throw new IllegalStateException("Unmapped domain exception", exception);
    }
}
