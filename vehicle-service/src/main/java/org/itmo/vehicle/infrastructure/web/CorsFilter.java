package org.itmo.vehicle.infrastructure.web;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.List;
import java.util.Set;

@Provider
@PreMatching
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String REQUEST_METHOD = "Access-Control-Request-Method";

    private final Set<String> allowedOrigins = Set.copyOf(ConfigProvider.getConfig()
            .getOptionalValues("soa.cors.allowed-origins", String.class)
            .orElse(List.of()));

    @Override
    public void filter(ContainerRequestContext request) {
        if (isPreflight(request)) {
            request.abortWith(Response.noContent().build());
        }
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        String origin = request.getHeaderString("Origin");
        if (origin == null || !allowedOrigins.contains(origin)) {
            return;
        }
        MultivaluedMap<String, Object> headers = response.getHeaders();
        headers.putSingle("Access-Control-Allow-Origin", origin);
        headers.add("Vary", "Origin");
        headers.putSingle("Access-Control-Expose-Headers", "Location");
        if (isPreflight(request)) {
            headers.putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
            headers.putSingle("Access-Control-Allow-Headers", "Content-Type");
            headers.putSingle("Access-Control-Max-Age", "3600");
        }
    }

    private static boolean isPreflight(ContainerRequestContext request) {
        return HttpMethod.OPTIONS.equals(request.getMethod()) && request.getHeaderString(REQUEST_METHOD) != null;
    }
}
