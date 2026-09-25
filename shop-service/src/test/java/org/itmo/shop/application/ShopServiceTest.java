package org.itmo.shop.application;

import org.itmo.shop.domain.*;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShopServiceTest {

    private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-09-10T08:19:00+03:00");

    private final InMemoryCatalog catalog = new InMemoryCatalog();
    private final ShopService shop = new ShopService(catalog);

    @Test
    void fixDistanceResetsMileageAndKeepsEverythingElse() {
        Vehicle tesla = catalog.add(vehicle(1, 15320.7));

        Vehicle fixed = shop.fixDistance(1);

        assertThat(fixed.mileage()).isZero();
        assertThat(fixed).usingRecursiveComparison().ignoringFields("mileage").isEqualTo(tesla);
        assertThat(catalog.written).containsExactly(fixed);
    }

    @Test
    void fixDistanceSetsUnknownMileageToZero() {
        catalog.add(vehicle(1, null));

        assertThat(shop.fixDistance(1).mileage()).isZero();
        assertThat(catalog.written).hasSize(1);
    }

    @Test
    void fixDistanceDoesNotWriteWhenOdometerAlreadyShowsZero() {
        Vehicle parked = catalog.add(vehicle(1, 0.0));

        assertThat(shop.fixDistance(1)).isEqualTo(parked);
        assertThat(catalog.written).isEmpty();
    }

    @Test
    void fixDistanceOfUnknownVehicleIsNotFound() {
        assertThatThrownBy(() -> shop.fixDistance(42))
                .isInstanceOf(VehicleNotFoundException.class)
                .hasMessage("Vehicle with id 42 does not exist.");
    }

    @Test
    void fixDistanceOfVehicleRemovedAfterReadingIsNotFound() {
        catalog.add(vehicle(1, 100.0));
        catalog.removeBeforeNextWrite = true;

        assertThatThrownBy(() -> shop.fixDistance(1)).isInstanceOf(VehicleNotFoundException.class);
    }

    @Test
    void searchReturnsWhatTheCatalogHolds() {
        catalog.add(vehicle(1, null));

        assertThat(shop.searchByType(VehicleType.CHOPPER)).extracting(Vehicle::id).containsExactly(1);
        assertThat(shop.searchByType(VehicleType.SPACESHIP)).isEmpty();
    }

    private static Vehicle vehicle(int id, Double mileage) {
        return new Vehicle(id, "Tesla", new Coordinates(1, 2), CREATED, 615.5f, 4, mileage,
                VehicleType.CHOPPER, FuelType.ELECTRICITY);
    }

    private static final class InMemoryCatalog implements VehicleCatalog {

        final Map<Integer, Vehicle> vehicles = new LinkedHashMap<>();
        final List<Vehicle> written = new ArrayList<>();
        boolean removeBeforeNextWrite;

        Vehicle add(Vehicle vehicle) {
            vehicles.put(vehicle.id(), vehicle);
            return vehicle;
        }

        @Override
        public Optional<Vehicle> findById(int id) {
            return Optional.ofNullable(vehicles.get(id));
        }

        @Override
        public List<Vehicle> findByType(VehicleType type) {
            return vehicles.values().stream().filter(vehicle -> vehicle.type() == type).toList();
        }

        @Override
        public Vehicle replace(Vehicle vehicle) {
            if (removeBeforeNextWrite) {
                vehicles.remove(vehicle.id());
            }
            if (!vehicles.containsKey(vehicle.id())) {
                throw new VehicleNotFoundException(vehicle.id());
            }
            written.add(vehicle);
            return add(vehicle);
        }
    }
}
