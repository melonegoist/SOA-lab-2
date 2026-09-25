package org.itmo.vehicle.application;

import org.itmo.vehicle.domain.*;
import org.itmo.vehicle.domain.query.Page;
import org.itmo.vehicle.domain.query.VehicleQuery;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VehicleServiceTest {

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-10T05:19:00.123456Z"), MOSCOW);
    private static final VehicleDetails TESLA =
            new VehicleDetails("Tesla", new Coordinates(1, 2), 615.5f, 4, 100.0, null, null);

    private final InMemoryVehicles vehicles = new InMemoryVehicles();
    private final CountingTransactions transactions = new CountingTransactions();
    private final VehicleService service = new VehicleService(vehicles, transactions, CLOCK);

    @Test
    void createsVehicleWithServerSideIdAndDate() {
        Vehicle created = service.create(TESLA);

        assertThat(created.getId()).isEqualTo(1);
        assertThat(created.getCreationDate()).isEqualTo(ZonedDateTime.parse("2026-09-10T08:19:00+03:00[Europe/Moscow]"));
        assertThat(transactions.count).isEqualTo(1);
    }

    @Test
    void replacesDetailsButKeepsIdentityAndDate() {
        Vehicle created = service.create(TESLA);
        VehicleDetails withoutMileage = new VehicleDetails("Tesla 2", new Coordinates(3, 4), 700f, null, null, null, null);

        Vehicle replaced = service.replace(created.getId(), withoutMileage);

        assertThat(replaced.getCreationDate()).isEqualTo(created.getCreationDate());
        assertThat(service.get(created.getId()).getDetails()).isEqualTo(withoutMileage);
    }

    @Test
    void reportsMissingVehicle() {
        assertThatThrownBy(() -> service.get(42)).isInstanceOf(VehicleNotFoundException.class);
        assertThatThrownBy(() -> service.replace(42, TESLA)).isInstanceOf(VehicleNotFoundException.class);
        assertThatThrownBy(() -> service.delete(42)).isInstanceOf(VehicleNotFoundException.class);
    }

    @Test
    void deletesVehicle() {
        Vehicle created = service.create(TESLA);

        service.delete(created.getId());

        assertThatThrownBy(() -> service.get(created.getId())).isInstanceOf(VehicleNotFoundException.class);
    }

    @Test
    void maxNameOfEmptyCollectionIsAnError() {
        assertThatThrownBy(service::vehicleWithMaxName).isInstanceOf(NoVehiclesException.class);
    }

    private static final class CountingTransactions implements TransactionRunner {
        private int count;

        @Override
        public <T> T inTransaction(Supplier<T> work) {
            count++;
            return work.get();
        }
    }

    private static final class InMemoryVehicles implements VehicleRepository {
        private final Map<Integer, Vehicle> rows = new LinkedHashMap<>();
        private int nextId = 1;

        @Override
        public Vehicle add(ZonedDateTime creationDate, VehicleDetails details) {
            Vehicle vehicle = new Vehicle(nextId++, creationDate, details);
            rows.put(vehicle.getId(), vehicle);
            return vehicle;
        }

        @Override
        public Optional<Vehicle> findById(int id) {
            return Optional.ofNullable(rows.get(id));
        }

        @Override
        public void update(Vehicle vehicle) {
            rows.put(vehicle.getId(), vehicle);
        }

        @Override
        public boolean remove(int id) {
            return rows.remove(id) != null;
        }

        @Override
        public Page<Vehicle> find(VehicleQuery query) {
            return new Page<>(List.copyOf(rows.values()), 1, 20, rows.size());
        }

        @Override
        public EnginePowerStatistics enginePowerStatistics() {
            return new EnginePowerStatistics(0, rows.size());
        }

        @Override
        public WheelsStatistics wheelsStatistics() {
            return new WheelsStatistics(null, 0);
        }

        @Override
        public Optional<Vehicle> findWithMaxName() {
            return rows.values().stream().max((a, b) -> a.getDetails().name().compareTo(b.getDetails().name()));
        }
    }
}
