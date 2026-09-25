package org.itmo.vehicle.application;

import org.itmo.vehicle.domain.*;
import org.itmo.vehicle.domain.query.Page;
import org.itmo.vehicle.domain.query.VehicleQuery;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class VehicleService {

    private final VehicleRepository vehicles;
    private final TransactionRunner transactions;
    private final Clock clock;

    public VehicleService(VehicleRepository vehicles, TransactionRunner transactions, Clock clock) {
        this.vehicles = Objects.requireNonNull(vehicles, "vehicles");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Vehicle create(VehicleDetails details) {
        ZonedDateTime now = ZonedDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        return transactions.inTransaction(() -> vehicles.add(now, details));
    }

    public Vehicle get(int id) {
        return vehicles.findById(id).orElseThrow(() -> new VehicleNotFoundException(id));
    }

    public Vehicle replace(int id, VehicleDetails details) {
        return transactions.inTransaction(() -> {
            Vehicle replaced = get(id).withDetails(details);
            vehicles.update(replaced);
            return replaced;
        });
    }

    public void delete(int id) {
        transactions.inTransaction(() -> {
            if (!vehicles.remove(id)) {
                throw new VehicleNotFoundException(id);
            }
            return null;
        });
    }

    public Page<Vehicle> find(VehicleQuery query) {
        return transactions.inTransaction(() -> vehicles.find(query));
    }

    public EnginePowerStatistics enginePowerStatistics() {
        return vehicles.enginePowerStatistics();
    }

    public WheelsStatistics wheelsStatistics() {
        return vehicles.wheelsStatistics();
    }

    public Vehicle vehicleWithMaxName() {
        return vehicles.findWithMaxName().orElseThrow(NoVehiclesException::new);
    }
}
