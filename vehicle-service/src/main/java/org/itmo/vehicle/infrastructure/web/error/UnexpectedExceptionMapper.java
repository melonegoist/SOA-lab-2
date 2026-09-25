package org.itmo.vehicle.infrastructure.web.error;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class UnexpectedExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(UnexpectedExceptionMapper.class.getName());

    @Context
    private UriInfo request;

    @Override
    public Response toResponse(Throwable exception) {
        LOG.log(Level.SEVERE, "Unexpected error while serving " + request.getRequestUri(), exception);
        return Problems.response(500, "Internal service error.", request);
    }
}
