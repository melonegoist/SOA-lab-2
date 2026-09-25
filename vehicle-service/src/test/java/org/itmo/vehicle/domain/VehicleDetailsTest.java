package org.itmo.vehicle.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VehicleDetailsTest {

    private static final Coordinates ORIGIN = new Coordinates(0, 0);

    @Test
    void acceptsMinimalVehicle() {
        VehicleDetails details = new VehicleDetails("Tesla", ORIGIN, 1f, null, null, null, null);

        assertThat(details.name()).isEqualTo("Tesla");
    }

    @Test
    void acceptsZeroMileage() {
        VehicleDetails details = new VehicleDetails("Tesla", ORIGIN, 1f, 4, 0.0, VehicleType.CHOPPER, FuelType.MANPOWER);

        assertThat(details.mileage()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n"})
    void rejectsBlankName(String name) {
        assertViolation(() -> new VehicleDetails(name, ORIGIN, 1f, null, null, null, null), "name");
    }

    @Test
    void rejectsXAbove471() {
        assertViolation(() -> new Coordinates(472, 0), "coordinates.x");
    }

    @Test
    void accepts471() {
        assertThat(new Coordinates(471, -1_000).x()).isEqualTo(471);
    }

    @ParameterizedTest
    @ValueSource(floats = {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY})
    void rejectsEnginePowerThatIsNotPositiveAndFinite(float enginePower) {
        assertViolation(() -> new VehicleDetails("Tesla", ORIGIN, enginePower, null, null, null, null), "enginePower");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -4})
    void rejectsNonPositiveNumberOfWheels(int wheels) {
        assertViolation(() -> new VehicleDetails("Tesla", ORIGIN, 1f, wheels, null, null, null), "numberOfWheels");
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, Double.POSITIVE_INFINITY, Double.NaN})
    void rejectsInvalidMileage(double mileage) {
        assertViolation(() -> new VehicleDetails("Tesla", ORIGIN, 1f, null, mileage, null, null), "mileage");
    }

    @Test
    void replacementKeepsIdentityAndCreationDate() {
        var created = new Vehicle(7, java.time.ZonedDateTime.parse("2026-09-10T08:19:00+03:00"),
                new VehicleDetails("Old", ORIGIN, 1f, null, 10.0, null, null));

        Vehicle replaced = created.withDetails(new VehicleDetails("New", ORIGIN, 2f, null, null, null, null));

        assertThat(replaced.getId()).isEqualTo(7);
        assertThat(replaced.getCreationDate()).isEqualTo(created.getCreationDate());
        assertThat(replaced.getDetails().name()).isEqualTo("New");
        assertThat(replaced).isEqualTo(created);
    }

    private static void assertViolation(Runnable construction, String field) {
        assertThatThrownBy(construction::run)
                .isInstanceOfSatisfying(InvariantViolationException.class,
                        e -> assertThat(e.getField()).isEqualTo(field));
    }
}
