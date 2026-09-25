package org.itmo.vehicle.domain;

import org.itmo.vehicle.domain.query.Page;
import org.itmo.vehicle.domain.query.VehicleQuery;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface VehicleRepository {

    Vehicle add(ZonedDateTime creationDate, VehicleDetails details);

    Optional<Vehicle> findById(int id);

    void update(Vehicle vehicle);

    boolean remove(int id);

    Page<Vehicle> find(VehicleQuery query);

    EnginePowerStatistics enginePowerStatistics();

    WheelsStatistics wheelsStatistics();

    Optional<Vehicle> findWithMaxName();
}
