package org.itmo.vehicle.infrastructure.web;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.itmo.vehicle.infrastructure.web.error.*;

import java.util.Map;
import java.util.Set;

@ApplicationPath("/")
public class VehicleServiceApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
                VehicleResource.class,
                StatisticsResource.class,
                JacksonConfiguration.class,
                CorsFilter.class,
                DomainExceptionMapper.class,
                InvalidParameterExceptionMapper.class,
                ConstraintViolationExceptionMapper.class,
                JsonProcessingExceptionMapper.class,
                WebApplicationExceptionMapper.class,
                UnexpectedExceptionMapper.class
        );
    }

    @Override
    @SuppressWarnings("deprecation") // getSingletons: fine for a stateless feature instance
    public Set<Object> getSingletons() {
        return Set.of(JacksonFeature.withoutExceptionMappers());
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of("jersey.config.jsonFeature", "JacksonFeature");
    }
}
