package org.itmo.shop.infrastructure.vehicleservice;

import org.itmo.shop.domain.*;
import org.itmo.shop.infrastructure.vehicleservice.generated.api.VehiclesApi;
import org.itmo.shop.infrastructure.vehicleservice.generated.model.VehicleDto;
import org.itmo.shop.infrastructure.vehicleservice.generated.model.VehiclePageDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import javax.net.ssl.SSLException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.itmo.shop.infrastructure.vehicleservice.VehicleClientMapper.toDomain;
import static org.itmo.shop.infrastructure.vehicleservice.VehicleClientMapper.toUpdateRequest;

@Component
class RestVehicleCatalog implements VehicleCatalog {

    static final int PAGE_SIZE = 100;

    private final VehiclesApi vehicles;

    RestVehicleCatalog(VehiclesApi vehicles) {
        this.vehicles = vehicles;
    }

    @Override
    public Optional<Vehicle> findById(int id) {
        try {
            return Optional.of(toDomain(body(vehicles.getVehicleById(id))));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw failure("read vehicle " + id, e);
        }
    }

    @Override
    public Vehicle replace(Vehicle vehicle) {
        try {
            return toDomain(body(vehicles.updateVehicle(vehicle.id(), toUpdateRequest(vehicle))));
        } catch (HttpClientErrorException.NotFound e) {
            throw new VehicleNotFoundException(vehicle.id());
        } catch (RestClientException e) {
            throw failure("update vehicle " + vehicle.id(), e);
        }
    }

    @Override
    public List<Vehicle> findByType(VehicleType type) {
        List<Vehicle> found = new ArrayList<>();
        int lastId = 0;
        List<VehicleDto> page;
        do {
            page = pageAfter(type, lastId);
            for (VehicleDto item : page) {
                Vehicle vehicle = toDomain(item);
                if (vehicle.id() <= lastId) {
                    throw new VehicleCatalogException("Could not search vehicles of type " + type
                            + ": the vehicle service returned them out of order.", null);
                }
                found.add(vehicle);
                lastId = vehicle.id();
            }
        } while (page.size() == PAGE_SIZE);
        return found;
    }

    private List<VehicleDto> pageAfter(VehicleType type, int lastId) {
        try {
            VehiclePageDto page = body(vehicles.getVehicles(
                    List.of("type:eq:" + type.name(), "id:gt:" + lastId), List.of("id"), 1, PAGE_SIZE));
            if (page.getItems() == null) {
                throw new RestClientException("The page has no items");
            }
            return page.getItems();
        } catch (RestClientException e) {
            throw failure("search vehicles of type " + type, e);
        }
    }

    private static <T> T body(ResponseEntity<T> response) {
        if (response.getBody() == null) {
            throw new RestClientException("The response has no body");
        }
        return response.getBody();
    }

    private static VehicleCatalogException failure(String action, RestClientException e) {
        return new VehicleCatalogException("Could not " + action + ": " + reason(e) + ".", e);
    }

    private static String reason(RestClientException e) {
        if (e instanceof RestClientResponseException response) {
            return "the vehicle service answered with status " + response.getStatusCode().value();
        }
        if (e instanceof ResourceAccessException) {
            if (causedBy(e, HttpTimeoutException.class) || causedBy(e, SocketTimeoutException.class)) {
                return "the vehicle service did not answer in time";
            }
            if (causedBy(e, SSLException.class)) {
                return "no trusted connection to the vehicle service could be established";
            }
            return "the vehicle service is unreachable";
        }
        return "the vehicle service returned a response the shop cannot read";
    }

    private static boolean causedBy(Throwable e, Class<? extends Throwable> type) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (type.isInstance(cause)) {
                return true;
            }
        }
        return false;
    }
}
