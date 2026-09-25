package org.itmo.vehicle.infrastructure.web.error;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.ParamException;

import java.util.List;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Context
    private UriInfo request;

    @Override
    public Response toResponse(WebApplicationException exception) {
        if (exception instanceof ParamException parameter) {
            String name = parameter.getParameterName();
            return Problems.response(400, "Parameter '" + name + "' has an invalid value.", request,
                    List.of(Problems.error(name, "is not a valid value of this parameter")));
        }
        Response original = exception.getResponse();
        int status = original.getStatus();
        Response problem = Problems.response(status, detail(status, exception), request);
        String allow = original.getHeaderString(HttpHeaders.ALLOW);
        return allow == null ? problem : Response.fromResponse(problem).header(HttpHeaders.ALLOW, allow).build();
    }

    private static String detail(int status, WebApplicationException exception) {
        return switch (status) {
            case 404 -> "There is no resource at this address.";
            case 405 -> "The resource does not support this method.";
            case 406 -> "The resource cannot produce the requested media type.";
            case 415 -> "The request body must be application/json.";
            default -> exception.getMessage();
        };
    }
}
