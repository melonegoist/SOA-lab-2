package org.itmo.shop.infrastructure.vehicleservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.itmo.shop.domain.*;
import org.itmo.shop.infrastructure.vehicleservice.generated.api.VehiclesApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.ConnectException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpTimeoutException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RestVehicleCatalogTest {

    private static final String BASE = "/vehicle-service";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://vehicles.test" + BASE);
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final RestVehicleCatalog catalog = new RestVehicleCatalog(HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(builder.build()))
            .build()
            .createClient(VehiclesApi.class));

    @AfterEach
    void allExpectedRequestsWereSent() {
        server.verify();
    }

    @Test
    void searchFollowsIdsAcrossPagesUntilAnIncompletePage() {
        server.expect(get("/vehicles?filter=type:eq:CHOPPER&filter=id:gt:0&sort=id&page=1&size=100"))
                .andRespond(page(IntStream.rangeClosed(1, 100)));
        server.expect(get("/vehicles?filter=type:eq:CHOPPER&filter=id:gt:100&sort=id&page=1&size=100"))
                .andRespond(page(IntStream.of(250)));

        List<Vehicle> found = catalog.findByType(VehicleType.CHOPPER);

        assertThat(found).hasSize(101);
        assertThat(found.get(100).id()).isEqualTo(250);
    }

    @Test
    void searchAsksOnceMoreAfterAFullPage() {
        server.expect(get("/vehicles?filter=type:eq:SPACESHIP&filter=id:gt:0&sort=id&page=1&size=100"))
                .andRespond(page(IntStream.rangeClosed(1, 100)));
        server.expect(get("/vehicles?filter=type:eq:SPACESHIP&filter=id:gt:100&sort=id&page=1&size=100"))
                .andRespond(page(IntStream.empty()));

        assertThat(catalog.findByType(VehicleType.SPACESHIP)).hasSize(100);
    }

    @Test
    void searchStopsWhenIdsDoNotGrow() {
        server.expect(get("/vehicles?filter=type:eq:CHOPPER&filter=id:gt:0&sort=id&page=1&size=100"))
                .andRespond(page(IntStream.of(5, 3)));

        assertThatThrownBy(() -> catalog.findByType(VehicleType.CHOPPER))
                .isInstanceOf(VehicleCatalogException.class)
                .hasMessage("Could not search vehicles of type CHOPPER: the vehicle service returned them out of order.");
    }

    @Test
    void searchRejectedByTheVehicleServiceIsAFailure() {
        server.expect(get("/vehicles?filter=type:eq:CHOPPER&filter=id:gt:0&sort=id&page=1&size=100"))
                .andRespond(problem(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> catalog.findByType(VehicleType.CHOPPER))
                .isInstanceOf(VehicleCatalogException.class)
                .hasMessage("Could not search vehicles of type CHOPPER: the vehicle service answered with status 400.");
    }

    @Test
    void findsVehicleById() {
        server.expect(get("/vehicles/7")).andRespond(json(vehicle(7, "\"CHOPPER\"")));

        Vehicle vehicle = catalog.findById(7).orElseThrow();

        assertThat(vehicle.name()).isEqualTo("Tesla");
        assertThat(vehicle.coordinates()).isEqualTo(new Coordinates(1, 2));
        assertThat(vehicle.creationDate().toInstant()).isEqualTo(OffsetDateTime.parse("2026-09-10T08:19:00+03:00").toInstant());
        assertThat(vehicle.enginePower()).isEqualTo(615.5f);
        assertThat(vehicle.mileage()).isEqualTo(15320.7);
        assertThat(vehicle.type()).isEqualTo(VehicleType.CHOPPER);
    }

    @Test
    void missingVehicleIsEmpty() {
        server.expect(get("/vehicles/7")).andRespond(problem(HttpStatus.NOT_FOUND));

        assertThat(catalog.findById(7)).isEmpty();
    }

    @Test
    void replaceSendsTheWholeVehicle() {
        server.expect(request -> {
                    assertThat(request.getMethod()).isEqualTo(HttpMethod.PUT);
                    assertThat(request.getURI().getPath()).isEqualTo(BASE + "/vehicles/7");
                    assertThat(JSON.readTree(((MockClientHttpRequest) request).getBodyAsString())).isEqualTo(JSON.readTree("""
                            {"name":"Tesla","coordinates":{"x":1,"y":2},"enginePower":615.5,
                             "numberOfWheels":null,"mileage":0.0,"type":null,"fuelType":null}"""));
                })
                .andRespond(json(vehicle(7, "null").replace("15320.7", "0.0")));

        Vehicle stored = catalog.replace(new Vehicle(7, "Tesla", new Coordinates(1, 2),
                OffsetDateTime.parse("2026-09-10T08:19:00+03:00"), 615.5f, null, 0.0, null, null));

        assertThat(stored.mileage()).isZero();
    }

    @Test
    void replacingVehicleThatIsGoneIsNotFound() {
        server.expect(request -> assertThat(request.getMethod()).isEqualTo(HttpMethod.PUT))
                .andRespond(problem(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> catalog.replace(new Vehicle(7, "Tesla", new Coordinates(1, 2),
                OffsetDateTime.now(), 1f, null, 0.0, null, null)))
                .isInstanceOf(VehicleNotFoundException.class);
    }

    @Test
    void serverErrorOfTheVehicleService() {
        server.expect(get("/vehicles/7")).andRespond(withServerError());

        assertFailure("Could not read vehicle 7: the vehicle service answered with status 500.");
    }

    @Test
    void vehicleServiceUnreachable() {
        server.expect(get("/vehicles/7")).andRespond(withException(new ConnectException("Connection refused")));

        assertFailure("Could not read vehicle 7: the vehicle service is unreachable.");
    }

    @Test
    void vehicleServiceTooSlow() {
        server.expect(get("/vehicles/7")).andRespond(withException(new HttpTimeoutException("request timed out")));

        assertFailure("Could not read vehicle 7: the vehicle service did not answer in time.");
    }

    @Test
    void unknownEnumValueIsNotReadAsNull() {
        server.expect(get("/vehicles/7")).andRespond(json(vehicle(7, "\"TANK\"")));

        assertFailure("Could not read vehicle 7: the vehicle service returned a response the shop cannot read.");
    }

    @Test
    void vehicleWithoutRequiredField() {
        server.expect(get("/vehicles/7")).andRespond(json(vehicle(7, "null").replace("\"name\":\"Tesla\",", "")));

        assertFailure("The vehicle service returned a vehicle without name.");
    }

    private void assertFailure(String message) {
        assertThatThrownBy(() -> catalog.findById(7))
                .isInstanceOf(VehicleCatalogException.class)
                .hasMessage(message);
    }

    private static RequestMatcher get(String pathAndQuery) {
        return request -> {
            assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
            URI uri = request.getURI();
            String query = uri.getRawQuery() == null ? "" : "?" + URLDecoder.decode(uri.getRawQuery(), UTF_8);
            assertThat(uri.getPath() + query).isEqualTo(BASE + pathAndQuery);
        };
    }

    private static String vehicle(int id, String type) {
        return """
                {"id":%d,"name":"Tesla","coordinates":{"x":1,"y":2},"creationDate":"2026-09-10T08:19:00+03:00",
                 "enginePower":615.5,"numberOfWheels":null,"mileage":15320.7,"type":%s,"fuelType":null}"""
                .formatted(id, type);
    }

    private static ResponseCreator page(IntStream ids) {
        List<String> items = ids.mapToObj(id -> vehicle(id, "\"CHOPPER\"")).toList();
        return json("{\"items\":[" + String.join(",", items) + "],\"page\":1,\"size\":100,"
                + "\"totalElements\":" + items.size() + ",\"totalPages\":1}");
    }

    private static ResponseCreator json(String body) {
        return withSuccess(body, MediaType.APPLICATION_JSON);
    }

    private static ResponseCreator problem(HttpStatus status) {
        return withStatus(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body("{\"type\":\"about:blank\",\"title\":\"" + status.getReasonPhrase() + "\",\"status\":" + status.value() + "}");
    }
}
