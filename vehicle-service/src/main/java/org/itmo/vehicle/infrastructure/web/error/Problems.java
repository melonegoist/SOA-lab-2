package org.itmo.vehicle.infrastructure.web.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.itmo.vehicle.infrastructure.web.generated.model.ProblemDto;
import org.itmo.vehicle.infrastructure.web.generated.model.ValidationErrorDto;

import java.util.List;

final class Problems {

    static final MediaType PROBLEM_JSON = new MediaType("application", "problem+json");

    private Problems() {
    }

    static Response response(int status, String detail, UriInfo request, List<ValidationErrorDto> errors) {
        Response.Status known = Response.Status.fromStatusCode(status);
        ProblemDto problem = new ProblemDto()
                .title(known != null ? known.getReasonPhrase() : "HTTP " + status)
                .status(status)
                .detail(detail)
                .instance(request == null ? null : request.getRequestUri().getRawPath())
                .errors(errors);
        return Response.status(status).type(PROBLEM_JSON).entity(problem).build();
    }

    static Response response(int status, String detail, UriInfo request) {
        return response(status, detail, request, List.of());
    }

    static ValidationErrorDto error(String field, String message) {
        return new ValidationErrorDto().field(field).message(message);
    }

    static String pointer(String propertyPath) {
        return propertyPath.replace('.', '/');
    }
}
