package org.itmo.shop.infrastructure.web.error;

import jakarta.servlet.http.HttpServletRequest;
import org.itmo.shop.domain.VehicleCatalogException;
import org.itmo.shop.domain.VehicleNotFoundException;
import org.itmo.shop.infrastructure.web.generated.model.ValidationErrorDto;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestControllerAdvice
class ProblemHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = Logger.getLogger(ProblemHandler.class.getName());

    private static final String INTERNAL_ERROR = "Internal service error.";
    private static final Map<Integer, String> STANDARD_DETAILS = Map.of(
            404, "There is no resource at this address.",
            405, "The resource does not support this method.",
            406, "The resource cannot produce the requested media type.",
            500, INTERNAL_ERROR);

    @ExceptionHandler
    ProblemDetail vehicleNotFound(VehicleNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler
    ProblemDetail vehicleCatalogFailure(VehicleCatalogException exception, HttpServletRequest request) {
        LOG.log(Level.WARNING, exception.getMessage() + " Request: " + request.getRequestURI(), exception.getCause());
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
    }

    @ExceptionHandler
    ProblemDetail unexpected(Exception exception, HttpServletRequest request) {
        LOG.log(Level.SEVERE, "Unexpected error while serving " + request.getRequestURI(), exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ValidationErrorDto> errors = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> error(nameInRequest(result.getMethodParameter()), error.getDefaultMessage())))
                .sorted(Comparator.comparing(ValidationErrorDto::getField))
                .toList();
        return badRequest(exception, "The request violates constraints of the contract.", errors, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String name = exception.getPropertyName();
        return badRequest(exception, "Parameter '" + name + "' has an invalid value.",
                List.of(error(name, expected(exception.getRequiredType()))), headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (status.is5xxServerError()) {
            LOG.log(Level.SEVERE, "Error while serving " + request.getDescription(false), exception);
        }
        return super.handleExceptionInternal(exception, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String standard = STANDARD_DETAILS.get(status.value());
        if (body instanceof ProblemDetail problem && standard != null) {
            problem.setDetail(standard);
        }
        return super.createResponseEntity(body, headers, status, request);
    }

    private ResponseEntity<Object> badRequest(Exception exception, String detail, List<ValidationErrorDto> errors,
                                              HttpHeaders headers, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setProperty("errors", errors);
        return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    private static ValidationErrorDto error(String field, String message) {
        return new ValidationErrorDto().field(field).message(message);
    }

    private static String nameInRequest(MethodParameter parameter) {
        PathVariable path = parameter.getParameterAnnotation(PathVariable.class);
        if (path != null && !path.value().isEmpty()) {
            return path.value();
        }
        RequestParam query = parameter.getParameterAnnotation(RequestParam.class);
        if (query != null && !query.value().isEmpty()) {
            return query.value();
        }
        return parameter.getParameterName();
    }

    private static String expected(Class<?> type) {
        if (type != null && type.isEnum()) {
            // the generated enums print their contract value
            return Arrays.stream(type.getEnumConstants())
                    .map(String::valueOf)
                    .collect(Collectors.joining(", ", "must be one of ", ""));
        }
        if (type == Integer.class || type == int.class) {
            return "must be a 32-bit integer";
        }
        return "has an invalid value";
    }
}
