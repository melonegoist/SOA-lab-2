package org.itmo.vehicle.infrastructure.persistence;

import jakarta.persistence.*;
import org.itmo.vehicle.domain.FuelType;
import org.itmo.vehicle.domain.VehicleType;

import java.time.OffsetDateTime;

@Entity
@Table(name = "soa_vehicle")
public class VehicleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Embedded
    private CoordinatesEmbeddable coordinates;

    @Column(name = "creation_date", nullable = false, updatable = false)
    private OffsetDateTime creationDate;

    @Column(name = "engine_power", nullable = false)
    private double enginePower;

    @Column(name = "number_of_wheels")
    private Integer numberOfWheels;

    @Column(name = "mileage")
    private Double mileage;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private VehicleType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type")
    private FuelType fuelType;

    protected VehicleEntity() {
        // for JPA
    }

    public VehicleEntity(OffsetDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CoordinatesEmbeddable getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(CoordinatesEmbeddable coordinates) {
        this.coordinates = coordinates;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public double getEnginePower() {
        return enginePower;
    }

    public void setEnginePower(double enginePower) {
        this.enginePower = enginePower;
    }

    public Integer getNumberOfWheels() {
        return numberOfWheels;
    }

    public void setNumberOfWheels(Integer numberOfWheels) {
        this.numberOfWheels = numberOfWheels;
    }

    public Double getMileage() {
        return mileage;
    }

    public void setMileage(Double mileage) {
        this.mileage = mileage;
    }

    public VehicleType getType() {
        return type;
    }

    public void setType(VehicleType type) {
        this.type = type;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }
}
