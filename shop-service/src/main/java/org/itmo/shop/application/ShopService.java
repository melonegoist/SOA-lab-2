package org.itmo.shop.application;

import org.itmo.shop.domain.Vehicle;
import org.itmo.shop.domain.VehicleCatalog;
import org.itmo.shop.domain.VehicleNotFoundException;
import org.itmo.shop.domain.VehicleType;

import java.util.List;

public final class ShopService {

    private final VehicleCatalog vehicles;

    public ShopService(VehicleCatalog vehicles) {
        this.vehicles = vehicles;
    }

    public List<Vehicle> searchByType(VehicleType type) {
        return vehicles.findByType(type);
    }

    public Vehicle fixDistance(int vehicleId) {
        Vehicle vehicle = vehicles.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
        if (vehicle.odometerShowsZero()) {
            return vehicle;
        }
        return vehicles.replace(vehicle.withOdometerReset());
    }
}
