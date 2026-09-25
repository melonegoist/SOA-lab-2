package org.itmo.vehicle.infrastructure.web.error;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.itmo.vehicle.infrastructure.web.InvalidParameterException;

import java.util.List;

@Provider
public class InvalidParameterExceptionMapper implements ExceptionMapper<InvalidParameterException> {

    @Context
    private UriInfo request;

    @Override
    public Response toResponse(InvalidParameterException exception) {
        return Problems.response(400, "A query parameter is invalid.", request,
                List.of(Problems.error(exception.getParameter(), exception.getMessage())));
    }
}
