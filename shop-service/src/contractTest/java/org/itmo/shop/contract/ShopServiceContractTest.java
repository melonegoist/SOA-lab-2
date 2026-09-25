package org.itmo.shop.contract;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.itmo.contract.ContractClient;
import org.itmo.contract.ContractClient.Reply;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The shop service against its contract, end to end: through the deployed
 * shop, the deployed vehicle service and its database. Every response of
 * both services is validated against its OpenAPI document by the clients.
 * <p>
 * Vehicles are created through the vehicle service with a run tag in their
 * name and removed at the end. The collection may hold other vehicles, so
 * the searches only check what they must contain and what they must not.
 */
class ShopServiceContractTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ContractClient shop = ContractClient.forService("shop");
    private static final ContractClient vehicles = ContractClient.forService("vehicle");
    private static final String RUN = "st" + Long.toString(System.nanoTime(), 36);
    private static final List<Integer> createdIds = new ArrayList<>();

    @AfterAll
    static void removeCreatedVehicles() {
        createdIds.forEach(id -> vehicles.unvalidated("DELETE", "/vehicles/" + id, null, null));
    }

    // ------------------------------------------------------------ search by type

    /**
     * More vehicles than one page of the vehicle service holds (100).
     */
    @Test
    void searchFindsEveryVehicleOfTheTypeAcrossPages() {
        List<Integer> mine = IntStream.range(0, 101)
                .mapToObj(i -> create("page-" + i, "HELICOPTER", null).get("id").asInt())
                .toList();

        Reply reply = shop.get("/shop/search/by-type/HELICOPTER");

        assertThat(reply.status()).as(reply.toString()).isEqualTo(200);
        List<JsonNode> found = elements(reply.json());
        assertThat(found).allSatisfy(vehicle -> assertThat(vehicle.get("type").asText()).isEqualTo("HELICOPTER"));
        assertThat(found).extracting(vehicle -> vehicle.get("id").asInt())
                .doesNotHaveDuplicates()
                .containsAll(mine);
    }

    @Test
    void searchReturnsVehiclesExactlyAsTheVehicleServiceHasThem() {
        int id = create("full", "CHOPPER", 15320.7).get("id").asInt();

        JsonNode found = elements(shop.get("/shop/search/by-type/CHOPPER").json()).stream()
                .filter(vehicle -> vehicle.get("id").asInt() == id)
                .findFirst()
                .orElseThrow();

        // the same fields, values, float power and date with its offset
        assertThat(found).isEqualTo(vehicles.get("/vehicles/" + id).json());
    }

    @ParameterizedTest
    @ValueSource(strings = {"TANK", "chopper", "CHOPPER%20", "%20CHOPPER", "CHOPPER%09"})
    void searchRejectsValuesOutsideTheEnumeration(String type) {
        Reply reply = shop.get("/shop/search/by-type/" + type);

        assertThat(reply.status()).as(reply.toString()).isEqualTo(400);
        assertThat(reply.json().get("errors")).singleElement().satisfies(error -> {
            assertThat(error.get("field").asText()).isEqualTo("type");
            assertThat(error.get("message").asText()).isEqualTo("must be one of HELICOPTER, MOTORCYCLE, CHOPPER, SPACESHIP");
        });
    }

    // ------------------------------------------------------------ fix distance

    @Test
    void fixDistanceResetsOdometerAndKeepsTheRest() {
        JsonNode before = create("fix", "MOTORCYCLE", 15320.7);
        int id = before.get("id").asInt();

        Reply reply = shop.post("/shop/fix-distance/" + id);

        assertThat(reply.status()).as(reply.toString()).isEqualTo(200);
        assertThat(reply.json().get("mileage").asDouble()).isZero();
        assertThat(withoutMileage(reply.json())).isEqualTo(withoutMileage(before));
        assertThat(vehicles.get("/vehicles/" + id).json()).isEqualTo(reply.json());
    }

    @Test
    void fixDistanceSetsAbsentMileageToZero() {
        int id = create("unknown-mileage", null, null).get("id").asInt();

        Reply reply = shop.post("/shop/fix-distance/" + id);

        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.json().get("mileage").isNumber()).isTrue();
        assertThat(reply.json().get("mileage").asDouble()).isZero();
    }

    @Test
    void fixDistanceIsRepeatable() {
        int id = create("twice", null, 42.0).get("id").asInt();

        JsonNode first = shop.post("/shop/fix-distance/" + id).json();
        Reply second = shop.post("/shop/fix-distance/" + id);

        assertThat(second.status()).isEqualTo(200);
        assertThat(second.json()).isEqualTo(first);
    }

    @Test
    void fixDistanceOfMissingVehicleIsNotFound() {
        Reply reply = shop.post("/shop/fix-distance/2147483647");

        assertThat(reply.status()).isEqualTo(404);
        assertThat(reply.json().get("detail").asText()).isEqualTo("Vehicle with id 2147483647 does not exist.");
        assertThat(reply.json().get("instance").asText()).endsWith("/shop/fix-distance/2147483647");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void fixDistanceRejectsNonPositiveIds(String id) {
        Reply reply = shop.post("/shop/fix-distance/" + id);

        assertThat(reply.status()).isEqualTo(400);
        assertThat(reply.json().get("errors")).singleElement().satisfies(error -> {
            assertThat(error.get("field").asText()).isEqualTo("vehicle-id");
            assertThat(error.get("message").asText()).isEqualTo("must be greater than or equal to 1");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1.5", "99999999999", "0x7FFFFFFF", "%202147483647", "2147%20483647"})
    void fixDistanceRejectsWhatIsNotAnInt32(String id) {
        Reply reply = shop.post("/shop/fix-distance/" + id);

        assertThat(reply.status()).isEqualTo(400);
        assertThat(reply.json().get("errors")).singleElement().satisfies(error -> {
            assertThat(error.get("field").asText()).isEqualTo("vehicle-id");
            assertThat(error.get("message").asText()).isEqualTo("must be a 32-bit integer");
        });
    }

    // ------------------------------------------------------------ outside the contract

    @Test
    void validationMessagesAreEnglishWhateverTheClientLanguage() throws JsonProcessingException {
        HttpResponse<String> response = shop.raw(HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Accept-Language", "ru-RU"), "/shop/fix-distance/0");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JSON.readTree(response.body()).at("/errors/0/message").asText())
                .isEqualTo("must be greater than or equal to 1");
    }

    @Test
    void errorsOutsideTheContractAreProblemsToo() {
        Reply unknownPath = shop.unvalidated("GET", "/nothing-here", null, null);
        assertThat(unknownPath.status()).isEqualTo(404);
        assertThat(unknownPath.contentType()).startsWith("application/problem+json");

        Reply wrongMethod = shop.unvalidated("GET", "/shop/fix-distance/1", null, null);
        assertThat(wrongMethod.status()).isEqualTo(405);
        assertThat(wrongMethod.contentType()).startsWith("application/problem+json");
        assertThat(wrongMethod.header("Allow")).isEqualTo("POST");
    }

    @Test
    void allowsTheWebClientOriginOnly() {
        HttpResponse<String> preflight = shop.raw(HttpRequest.newBuilder()
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "https://se.ifmo.ru")
                .header("Access-Control-Request-Method", "POST"), "/shop/fix-distance/1");
        assertThat(preflight.statusCode()).isEqualTo(204);
        assertThat(preflight.headers().firstValue("Access-Control-Allow-Origin")).hasValue("https://se.ifmo.ru");
        assertThat(preflight.headers().firstValue("Access-Control-Allow-Methods").orElse("")).contains("POST");

        HttpResponse<String> error = shop.raw(HttpRequest.newBuilder()
                .header("Origin", "https://se.ifmo.ru")
                .GET(), "/shop/search/by-type/TANK");
        assertThat(error.statusCode()).isEqualTo(400);
        assertThat(error.headers().firstValue("Access-Control-Allow-Origin")).hasValue("https://se.ifmo.ru");

        HttpResponse<String> foreign = shop.raw(HttpRequest.newBuilder()
                .header("Origin", "https://evil.example")
                .GET(), "/shop/search/by-type/CHOPPER");
        assertThat(foreign.statusCode()).isEqualTo(200);
        assertThat(foreign.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
    }

    // ------------------------------------------------------------ helpers

    /**
     * Creates a vehicle through the vehicle service; {@code name} gets the run tag.
     */
    private static JsonNode create(String name, String type, Double mileage) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", RUN + "-" + name);
        body.put("coordinates", Map.of("x", 471, "y", -47));
        body.put("enginePower", 100.1);
        body.put("numberOfWheels", 2);
        body.put("mileage", mileage);
        body.put("type", type);
        body.put("fuelType", "MANPOWER");
        Reply reply = vehicles.post("/vehicles", json(body));
        assertThat(reply.status()).as(reply.toString()).isEqualTo(201);
        JsonNode vehicle = reply.json();
        createdIds.add(vehicle.get("id").asInt());
        return vehicle;
    }

    private static String json(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<JsonNode> elements(JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false).toList();
    }

    private static JsonNode withoutMileage(JsonNode vehicle) {
        ObjectNode copy = vehicle.deepCopy();
        copy.remove("mileage");
        return copy;
    }
}
