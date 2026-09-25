package org.itmo.vehicle.infrastructure.web.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.itmo.vehicle.infrastructure.web.generated.model.ValidationErrorDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Map<String, String> PATTERN_MEANINGS = Map.of(
            "^\\s*\\S[\\s\\S]*$",
            "must contain a non-whitespace character",
            "^(id|name|coordinates\\.x|coordinates\\.y|creationDate|enginePower|numberOfWheels|mileage|type|fuelType)"
                    + ":(eq|neq|gt|gte|lt|lte|like|in|isnull):.+$",
            "must have the form field:operator:value with a known field and operator",
            "^-?(id|name|coordinates\\.x|coordinates\\.y|creationDate|enginePower|numberOfWheels|mileage|type|fuelType)$",
            "must be a field name, optionally prefixed with - for descending order");

    @Context
    private UriInfo request;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<ValidationErrorDto> errors = exception.getConstraintViolations().stream()
                .map(violation -> Problems.error(pointer(violation), message(violation)))
                .sorted(Comparator.comparing(ValidationErrorDto::getField))
                .toList();
        return Problems.response(400, "The request violates constraints of the contract.", request, errors);
    }

    private static String message(ConstraintViolation<?> violation) {
        if (violation.getConstraintDescriptor().getAnnotation() instanceof Pattern pattern) {
            return PATTERN_MEANINGS.getOrDefault(pattern.regexp(), violation.getMessage());
        }
        return violation.getMessage();
    }

    static String pointer(ConstraintViolation<?> violation) {
        List<Path.Node> nodes = new ArrayList<>();
        violation.getPropertyPath().forEach(nodes::add);
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            Path.Node node = nodes.get(i);
            boolean leaf = i == nodes.size() - 1;
            switch (node.getKind()) {
                case PARAMETER -> {
                    boolean bodyProperty = !leaf && nodes.get(i + 1).getKind() == jakarta.validation.ElementKind.PROPERTY;
                    boolean missingBody = leaf && isNotNull(violation);
                    if (!bodyProperty && !missingBody) {
                        parts.add(node.getName());
                    }
                }
                case PROPERTY -> {
                    if (node.getIndex() != null) {
                        parts.add(String.valueOf(node.getIndex()));
                    }
                    parts.add(node.getName());
                }
                case CONTAINER_ELEMENT -> {
                    if (node.getIndex() != null) {
                        parts.add(String.valueOf(node.getIndex()));
                    }
                }
                default -> {
                }
            }
        }
        return String.join("/", parts);
    }

    private static boolean isNotNull(ConstraintViolation<?> violation) {
        return violation.getConstraintDescriptor().getAnnotation() instanceof NotNull;
    }
}
