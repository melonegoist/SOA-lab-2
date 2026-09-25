package org.itmo.vehicle.infrastructure.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.itmo.vehicle.application.TransactionRunner;
import org.itmo.vehicle.application.VehicleService;
import org.itmo.vehicle.domain.VehicleRepository;

import java.time.Clock;

@ApplicationScoped
public class ApplicationWiring {

    @Produces
    @Singleton
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Produces
    @Singleton
    VehicleService vehicleService(VehicleRepository vehicles, TransactionRunner transactions, Clock clock) {
        return new VehicleService(vehicles, transactions, clock);
    }
}
