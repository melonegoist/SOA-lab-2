package org.itmo.vehicle.infrastructure.web;

import jakarta.ws.rs.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ResourcePathsTest {

    @ParameterizedTest
    @ValueSource(classes = {VehicleResource.class, StatisticsResource.class})
    void resourceRepeatsThePathOfItsGeneratedInterface(Class<?> resource) {
        Class<?> api = Arrays.stream(resource.getInterfaces())
                .filter(type -> type.getPackageName().endsWith(".generated.api"))
                .findFirst()
                .orElseThrow();

        assertThat(resource.getAnnotation(Path.class).value())
                .as("@Path of %s", resource.getSimpleName())
                .isEqualTo(api.getAnnotation(Path.class).value());
    }
}
