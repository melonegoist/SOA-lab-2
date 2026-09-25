package org.itmo.vehicle.infrastructure.web.error;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Provider
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {

    private static final Logger LOG = Logger.getLogger(JsonProcessingExceptionMapper.class.getName());

    @Context
    private UriInfo request;

    @Override
    public Response toResponse(JsonProcessingException exception) {
        if (exception.getProcessor() instanceof JsonGenerator) {
            LOG.log(Level.SEVERE, "Cannot serialize a response", exception);
            return Problems.response(500, "Internal service error.", request);
        }
        if (exception instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
            return Problems.response(400, "The request body does not match the schema.", request,
                    List.of(Problems.error(pointer(mapping), describe(mapping))));
        }
        return Problems.response(400, "The request body is not valid JSON: " + exception.getOriginalMessage(), request);
    }

    private static String pointer(JsonMappingException exception) {
        return exception.getPath().stream()
                .map(reference -> reference.getFieldName() != null
                        ? reference.getFieldName()
                        : String.valueOf(reference.getIndex()))
                .collect(Collectors.joining("/"));
    }

    private static String describe(JsonMappingException exception) {
        if (exception instanceof UnrecognizedPropertyException) {
            return "is not a property of this object";
        }
        if (exception instanceof ValueInstantiationException && exception.getCause() != null) {
            // e.g. an unknown enumeration member: "Unexpected value 'TANK'"
            return exception.getCause().getMessage();
        }
        if (exception instanceof InvalidFormatException format) {
            return "has an invalid value: " + format.getValue();
        }
        if (exception instanceof MismatchedInputException) {
            return "has a value of the wrong type";
        }
        return "has an invalid value";
    }
}
