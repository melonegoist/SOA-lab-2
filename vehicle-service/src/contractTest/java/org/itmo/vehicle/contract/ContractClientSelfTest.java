package org.itmo.vehicle.contract;

import com.atlassian.oai.validator.report.ValidationReport;
import org.itmo.contract.ContractClient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Guards the guard: a contract test that silently validates nothing would
 * pass for any service. Known-bad responses must be reported.
 */
class ContractClientSelfTest {

    private static final ContractClient api = ContractClient.forService("vehicle");
    private static final String VEHICLE = """
            {"id":42,"name":"Tesla","coordinates":{"x":1,"y":2},"creationDate":"2026-09-10T08:19:00+03:00",
             "enginePower":615.5,"numberOfWheels":null,"mileage":null,"type":null,"fuelType":null}""";

    static Stream<Arguments> responses() {
        return Stream.of(
                arguments("valid vehicle", 200, "application/json", VEHICLE, true),
                arguments("vehicle without name", 200, "application/json", VEHICLE.replace("\"name\":\"Tesla\",", ""), false),
                arguments("x above 471", 200, "application/json", VEHICLE.replace("\"x\":1", "\"x\":472"), false),
                arguments("unknown type", 200, "application/json", VEHICLE.replace("\"type\":null", "\"type\":\"TANK\""), false),
                arguments("date that is not RFC 3339", 200, "application/json",
                        VEHICLE.replace("+03:00\"", "+03:00[Europe/Moscow]\""), false),
                arguments("problem with errors: null", 404, "application/problem+json",
                        "{\"title\":\"Not Found\",\"status\":404,\"errors\":null}", false),
                arguments("undocumented status", 418, "application/json", "{}", false));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("responses")
    void validatorTellsGoodFromBad(String description, int status, String contentType, String body, boolean valid) {
        ValidationReport report = api.check("GET", "/vehicles/42", status, contentType, body);

        assertThat(report.hasErrors()).as(report.getMessages().toString()).isEqualTo(!valid);
    }
}
