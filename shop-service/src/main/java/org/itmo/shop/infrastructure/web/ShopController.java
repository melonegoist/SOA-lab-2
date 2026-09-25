package org.itmo.shop.infrastructure.web;

import org.itmo.shop.application.ShopService;
import org.itmo.shop.infrastructure.web.generated.api.ShopApi;
import org.itmo.shop.infrastructure.web.generated.model.VehicleDto;
import org.itmo.shop.infrastructure.web.generated.model.VehicleTypeDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class ShopController implements ShopApi {

    private final ShopService shop;

    ShopController(ShopService shop) {
        this.shop = shop;
    }

    @Override
    public ResponseEntity<List<VehicleDto>> searchVehiclesByType(VehicleTypeDto type) {
        List<VehicleDto> found = shop.searchByType(VehicleDtoMapper.toDomain(type)).stream()
                .map(VehicleDtoMapper::toDto)
                .toList();
        return ResponseEntity.ok(found);
    }

    @Override
    public ResponseEntity<VehicleDto> fixVehicleDistance(Integer vehicleId) {
        return ResponseEntity.ok(VehicleDtoMapper.toDto(shop.fixDistance(vehicleId)));
    }
}
