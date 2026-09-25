package org.itmo.shop.domain;

import java.util.List;
import java.util.Optional;

public interface VehicleCatalog {

    Optional<Vehicle> findById(int id);

    List<Vehicle> findByType(VehicleType type);

    Vehicle replace(Vehicle vehicle);
}
