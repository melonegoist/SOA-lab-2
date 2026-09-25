package org.itmo.vehicle.contract;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.itmo.contract.ContractClient;
import org.itmo.contract.ContractClient.Reply;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * The vehicle service against its contract. Every response of a documented
 * operation is validated against openapi/vehicle-service.yaml by the client;
 * the tests on top check the behaviour the schema cannot express.
 * <p>
 * The collection may hold other data: every vehicle created here carries a
 * run tag in its name, queries filter by it, and the vehicles are removed
 * at the end.
 */
class VehicleServiceContractTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ContractClient api = ContractClient.forService("vehicle");
    private static final String RUN = "ct" + Long.toString(System.nanoTime(), 36);
    private static final List<Integer> createdIds = new ArrayList<>();

    @AfterAll
    static void removeCreatedVehicles() {
        createdIds.forEach(id -> api.unvalidated("DELETE", "/vehicles/" + id, null, null));
    }

    // ------------------------------------------------------------ CRUD

    @Test
    void createsReadsReplacesAndDeletesVehicle() {
        Map<String, Object> body = vehicle("lifecycle");
        body.put("numberOfWheels", 4);
        body.put("mileage", 15320.7);
        body.put("type", "CHOPPER");
        body.put("fuelType", "ELECTRICITY");

        Reply post = api.post("/vehicles", json(body));
        assertThat(post.status()).as(post.toString()).isEqualTo(201);
        JsonNode created = post.json();
        int id = created.get("id").asInt();
        createdIds.add(id);
        assertThat(post.header("Location")).endsWith("/vehicles/" + id);
        assertThat(created.get("creationDate").asText())
                .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(Z|[+-]\\d{2}:\\d{2})");

        Reply get = api.get("/vehicles/" + id);
        assertThat(get.status()).isEqualTo(200);
        assertThat(get.json()).isEqualTo(created);

        // PUT replaces: omitted optional fields become null, identity and date stay
        Map<String, Object> replacement = new LinkedHashMap<>();
        replacement.put("name", RUN + "-replaced");
        replacement.put("coordinates", Map.of("x", 471, "y", -5));
        replacement.put("enginePower", 1.5);
        Reply put = api.put("/vehicles/" + id, json(replacement));
        assertThat(put.status()).as(put.toString()).isEqualTo(200);
        JsonNode replaced = put.json();
        assertThat(replaced.get("id")).isEqualTo(created.get("id"));
        assertThat(replaced.get("creationDate")).isEqualTo(created.get("creationDate"));
        assertThat(replaced.get("name").asText()).isEqualTo(RUN + "-replaced");
        assertThat(replaced.get("mileage").isNull()).isTrue();
        assertThat(replaced.get("type").isNull()).isTrue();
        assertThat(api.get("/vehicles/" + id).json()).isEqualTo(replaced);

        assertThat(api.delete("/vehicles/" + id).status()).isEqualTo(204);
        assertThat(api.get("/vehicles/" + id).status()).isEqualTo(404);
        assertThat(api.delete("/vehicles/" + id).status()).isEqualTo(404);
        assertThat(api.put("/vehicles/" + id, json(replacement)).status()).isEqualTo(404);
    }

    // ------------------------------------------------------ collection

    @Test
    void filtersSortsAndPagesTheCollection() {
        create(vehicle("fs-alpha", 10, 2, null, "CHOPPER"));
        create(vehicle("fs-Beta", 20, null, null, "SPACESHIP"));
        create(vehicle("fs-gamma", 30, 4, 0.0, null));
        create(vehicle("fs-delta", 20, 4, null, "CHOPPER"));
        String mine = mine("fs-");

        JsonNode all = api.get("/vehicles?" + mine + "&sort=-enginePower&sort=name").json();
        assertThat(names(all)).containsExactly("fs-gamma", "fs-Beta", "fs-delta", "fs-alpha");
        assertThat(all.get("totalElements").asLong()).isEqualTo(4);
        assertThat(all.get("totalPages").asInt()).isEqualTo(1);

        JsonNode secondPage = api.get("/vehicles?" + mine + "&sort=-enginePower&sort=name&size=3&page=2").json();
        assertThat(names(secondPage)).containsExactly("fs-alpha");
        assertThat(secondPage.get("page").asInt()).isEqualTo(2);
        assertThat(secondPage.get("size").asInt()).isEqualTo(3);
        assertThat(secondPage.get("totalPages").asInt()).isEqualTo(2);

        JsonNode beyond = api.get("/vehicles?" + mine + "&size=3&page=5").json();
        assertThat(beyond.get("items")).isEmpty();
        assertThat(beyond.get("totalElements").asLong()).isEqualTo(4);

        assertThat(count(mine + "&filter=name:like:BETA")).as("like ignores case").isEqualTo(1);
        assertThat(count(mine + "&filter=type:in:CHOPPER,SPACESHIP")).isEqualTo(3);
        assertThat(count(mine + "&filter=type:isnull:true")).isEqualTo(1);
        assertThat(count(mine + "&filter=type:neq:CHOPPER")).as("absent type matches no comparison").isEqualTo(1);
        assertThat(count(mine + "&filter=numberOfWheels:isnull:false&filter=numberOfWheels:gte:4")).isEqualTo(2);
        assertThat(count(mine + "&filter=mileage:eq:0")).isEqualTo(1);
        assertThat(count(mine + "&filter=enginePower:gt:15&filter=enginePower:lt:25")).isEqualTo(2);
        assertThat(count(mine + "&filter=name:like:fs-_")).as("_ is literal, not a wildcard").isZero();
    }

    @Test
    void pagesNeverRepeatOrSkipVehiclesWithEqualSortKeys() {
        for (int i = 0; i < 4; i++) {
            create(vehicle("same-" + i, 42, null, null, null));
        }
        Set<Integer> seen = new HashSet<>();
        for (int page = 1; page <= 4; page++) {
            JsonNode items = api.get("/vehicles?" + mine("same-") + "&sort=enginePower&size=1&page=" + page)
                    .json().get("items");
            assertThat(items).hasSize(1);
            seen.add(items.get(0).get("id").asInt());
        }
        assertThat(seen).hasSize(4);
    }

    @Test
    void filtersByCreationDateGivenInRfc3339() {
        JsonNode created = create(vehicle("dated"));
        String date = created.get("creationDate").asText();

        JsonNode found = api.get("/vehicles?" + mine("dated") + "&filter=creationDate:eq:"
                + URLEncoder.encode(date, UTF_8)).json();
        assertThat(ids(found)).contains(created.get("id").asInt());

        if (date.contains("+")) {
            // an unencoded + arrives as a space: explained in the error
            Reply unencoded = api.get("/vehicles?filter=creationDate:eq:" + date);
            assertThat(unencoded.status()).isEqualTo(400);
            assertThat(unencoded.json().get("errors").get(0).get("message").asText()).contains("%2B");
        }
    }

    @Test
    void emptySelectionIsNotAnError() {
        Reply reply = api.get("/vehicles?filter=name:eq:" + RUN + "-nobody");

        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.json().get("items")).isEmpty();
        assertThat(reply.json().get("totalPages").asInt()).isZero();
    }

    // ------------------------------------------------------ statistics

    @Test
    void sumsEnginePowerInDoublePrecision() {
        JsonNode before = api.get("/vehicles/statistics/engine-power-sum").json();

        create(vehicle("sum-a", 0.1, null, null, null));
        create(vehicle("sum-b", 0.2, null, null, null));

        JsonNode after = api.get("/vehicles/statistics/engine-power-sum").json();
        assertThat(after.get("consideredElements").asLong() - before.get("consideredElements").asLong()).isEqualTo(2);
        assertThat(after.get("sum").asDouble() - before.get("sum").asDouble()).isCloseTo(0.3, within(1e-6));
    }

    @Test
    void averagesOnlyDefinedNumbersOfWheels() {
        JsonNode before = api.get("/vehicles/statistics/number-of-wheels-average").json();

        create(vehicle("wheels-3", 1, 3, null, null));
        create(vehicle("wheels-none", 1, null, null, null));

        JsonNode after = api.get("/vehicles/statistics/number-of-wheels-average").json();
        long countBefore = before.get("consideredElements").asLong();
        double totalBefore = countBefore == 0 ? 0 : before.get("average").asDouble() * countBefore;
        assertThat(after.get("consideredElements").asLong()).isEqualTo(countBefore + 1);
        assertThat(after.get("average").asDouble() * (countBefore + 1)).isCloseTo(totalBefore + 3, within(1e-6));
    }

    @Test
    void findsMaximumNameInCodePointOrder() {
        // U+1F680 sorts after every BMP character in code point order
        Map<String, Object> rocket = vehicle("ignored");
        rocket.put("name", "🚀 " + RUN);
        JsonNode created = create(rocket);

        Reply reply = api.get("/vehicles/statistics/max-name");

        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.json().get("id")).isEqualTo(created.get("id"));
    }

    // ---------------------------------------------------------- errors

    static Stream<Arguments> invalidVehicles() {
        return Stream.of(
                arguments("blank name", with("name", "   "), "name"),
                arguments("missing name", without("name"), "name"),
                arguments("missing coordinates", without("coordinates"), "coordinates"),
                arguments("coordinates without y", with("coordinates", Map.of("x", 1)), "coordinates/y"),
                arguments("x above 471", with("coordinates", Map.of("x", 472, "y", 0)), "coordinates/x"),
                arguments("zero engine power", with("enginePower", 0), "enginePower"),
                arguments("engine power beyond float", with("enginePower", 1e39), "enginePower"),
                arguments("engine power as a string", with("enginePower", "615.5"), "enginePower"),
                arguments("zero wheels", with("numberOfWheels", 0), "numberOfWheels"),
                arguments("fractional wheels", with("numberOfWheels", 4.5), "numberOfWheels"),
                arguments("negative mileage", with("mileage", -0.5), "mileage"),
                arguments("unknown type", with("type", "TANK"), "type"),
                arguments("name as a number", with("name", 42), "name"),
                arguments("id sent by the client", with("id", 5), "id"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidVehicles")
    void rejectsInvalidVehicle(String description, Map<String, Object> body, String field) {
        Reply reply = api.post("/vehicles", json(body));

        assertThat(reply.status()).as(reply.toString()).isEqualTo(400);
        assertThat(reply.contentType()).startsWith("application/problem+json");
        assertThat(fields(reply.json())).as(reply.toString()).contains(field);
    }

    @Test
    void explainsViolationsForPeopleWhateverTheServerLocale() {
        Map<String, Object> body = with("name", "  ");
        body.put("coordinates", Map.of("x", 500, "y", 0));

        JsonNode problem = api.post("/vehicles", json(body)).json();

        Map<String, String> messages = new LinkedHashMap<>();
        problem.get("errors").forEach(error -> messages.put(error.get("field").asText(), error.get("message").asText()));
        assertThat(messages).containsEntry("name", "must contain a non-whitespace character");
        assertThat(messages).containsEntry("coordinates/x", "must be less than or equal to 471");
    }

    @Test
    void rejectsInvalidReplacementToo() {
        int id = create(vehicle("invalid-put")).get("id").asInt();

        Reply reply = api.put("/vehicles/" + id, json(with("coordinates", Map.of("x", 1000, "y", 0))));

        assertThat(reply.status()).isEqualTo(400);
        assertThat(fields(reply.json())).contains("coordinates/x");
    }

    @Test
    void rejectsMalformedAndMissingBodies() {
        assertThat(api.post("/vehicles", "{\"name\":").status()).isEqualTo(400);
        assertThat(api.post("/vehicles", "").status()).isEqualTo(400);
        assertThat(api.post("/vehicles", json(vehicle("trailing")) + " []").status()).isEqualTo(400);
    }

    @Test
    void rejectsBodyThatIsNotJson() {
        HttpResponse<String> response = api.raw(HttpRequest.newBuilder()
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("a vehicle")), "/vehicles");

        assertThat(response.statusCode()).isEqualTo(415);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
    }

    @Test
    void rejectsInvalidIdentifiers() {
        assertThat(api.get("/vehicles/abc").status()).isEqualTo(400);
        assertThat(api.get("/vehicles/0").status()).isEqualTo(400);
        assertThat(api.get("/vehicles/99999999999").status()).isEqualTo(400);
        assertThat(api.get("/vehicles/2147483647").status()).isEqualTo(404);
    }

    @Test
    void rejectsInvalidQueryParameters() {
        assertThat(fields(api.get("/vehicles?filter=name:gt:a").json())).containsExactly("filter/0");
        assertThat(fields(api.get("/vehicles?filter=name:like:a&filter=color:eq:red").json())).containsExactly("filter/1");
        assertThat(fields(api.get("/vehicles?filter=weight:eq:1").json())).containsExactly("filter/0");
        assertThat(fields(api.get("/vehicles?sort=-speed").json())).containsExactly("sort/0");
        assertThat(fields(api.get("/vehicles?size=101").json())).containsExactly("size");
        assertThat(fields(api.get("/vehicles?page=0").json())).containsExactly("page");
        assertThat(fields(api.get("/vehicles?size=abc").json())).containsExactly("size");
    }

    @Test
    void answersOutsideTheContractWithProblemDetailsToo() {
        Reply unknownPath = api.unvalidated("GET", "/nothing-here", null, null);
        assertThat(unknownPath.status()).isEqualTo(404);
        assertThat(unknownPath.contentType()).startsWith("application/problem+json");

        Reply wrongMethod = api.unvalidated("PATCH", "/vehicles/1", "application/json", "{}");
        assertThat(wrongMethod.status()).isEqualTo(405);
        assertThat(wrongMethod.header("Allow")).isNotNull();
        assertThat(wrongMethod.contentType()).startsWith("application/problem+json");
    }

    // ------------------------------------------------------------ CORS

    @Test
    void allowsTheWebClientOriginOnly() {
        HttpResponse<String> preflight = api.raw(HttpRequest.newBuilder()
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "https://localhost:28681")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type"), "/vehicles");
        assertThat(preflight.statusCode()).isBetween(200, 299);
        assertThat(preflight.headers().firstValue("Access-Control-Allow-Origin")).hasValue("https://localhost:28681");
        assertThat(preflight.headers().firstValue("Access-Control-Allow-Methods").orElse("")).contains("POST");

        HttpResponse<String> foreign = api.raw(HttpRequest.newBuilder()
                .header("Origin", "https://evil.example")
                .GET(), "/vehicles");
        assertThat(foreign.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
    }

    // --------------------------------------------------------- helpers

    private static JsonNode create(Map<String, Object> body) {
        Reply reply = api.post("/vehicles", json(body));
        assertThat(reply.status()).as(reply.toString()).isEqualTo(201);
        JsonNode vehicle = reply.json();
        createdIds.add(vehicle.get("id").asInt());
        return vehicle;
    }

    /**
     * Filter selecting this run's vehicles whose name starts with {@code prefix}.
     */
    private static String mine(String prefix) {
        return "filter=name:like:" + RUN + "-" + prefix;
    }

    private static long count(String query) {
        return api.get("/vehicles?" + query).json().get("totalElements").asLong();
    }

    /**
     * A valid request body; {@code name} gets the run tag.
     */
    private static Map<String, Object> vehicle(String name) {
        return vehicle(name, 100, null, null, null);
    }

    private static Map<String, Object> vehicle(String name, double power, Integer wheels, Double mileage, String type) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", RUN + "-" + name);
        body.put("coordinates", Map.of("x", 1, "y", 2));
        body.put("enginePower", power);
        if (wheels != null) {
            body.put("numberOfWheels", wheels);
        }
        if (mileage != null) {
            body.put("mileage", mileage);
        }
        if (type != null) {
            body.put("type", type);
        }
        return body;
    }

    private static Map<String, Object> with(String field, Object value) {
        Map<String, Object> body = vehicle("invalid");
        body.put(field, value);
        return body;
    }

    private static Map<String, Object> without(String field) {
        Map<String, Object> body = vehicle("invalid");
        body.remove(field);
        return body;
    }

    private static List<String> names(JsonNode page) {
        return StreamSupport.stream(page.get("items").spliterator(), false)
                .map(item -> item.get("name").asText().substring(RUN.length() + 1))
                .toList();
    }

    private static List<Integer> ids(JsonNode page) {
        return StreamSupport.stream(page.get("items").spliterator(), false)
                .map(item -> item.get("id").asInt())
                .toList();
    }

    private static List<String> fields(JsonNode problem) {
        JsonNode errors = problem.get("errors");
        if (errors == null) {
            return List.of();
        }
        return StreamSupport.stream(errors.spliterator(), false)
                .map(error -> error.get("field").asText())
                .toList();
    }

    private static String json(Object body) {
        try {
            return JSON.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
