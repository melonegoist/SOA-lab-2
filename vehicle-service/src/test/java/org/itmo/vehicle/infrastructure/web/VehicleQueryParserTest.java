package org.itmo.vehicle.infrastructure.web;

import org.itmo.vehicle.domain.VehicleType;
import org.itmo.vehicle.domain.query.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VehicleQueryParserTest {

    private final VehicleQueryParser parser = new VehicleQueryParser();

    @Test
    void parsesComparison() {
        assertThat(filter("enginePower:gt:100"))
                .isEqualTo(new Criterion.Comparison(VehicleField.ENGINE_POWER, Operator.GT, 100f));
    }

    @Test
    void parsesInListOfEnumMembers() {
        assertThat(filter("type:in:CHOPPER,SPACESHIP"))
                .isEqualTo(new Criterion.In(VehicleField.TYPE, List.of(VehicleType.CHOPPER, VehicleType.SPACESHIP)));
    }

    @Test
    void parsesLikeAndIsNull() {
        assertThat(filter("name:like:tes:la")).isEqualTo(new Criterion.Like(VehicleField.NAME, "tes:la"));
        assertThat(filter("numberOfWheels:isnull:false")).isEqualTo(new Criterion.IsNull(VehicleField.NUMBER_OF_WHEELS, false));
    }

    @Test
    void parsesDateWithColonsInTheValue() {
        assertThat(filter("creationDate:gte:2026-09-10T08:19:00+03:00"))
                .isEqualTo(new Criterion.Comparison(VehicleField.CREATION_DATE, Operator.GTE,
                        ZonedDateTime.parse("2026-09-10T08:19:00+03:00")));
    }

    @Test
    void parsesNestedField() {
        assertThat(filter("coordinates.x:lte:-5"))
                .isEqualTo(new Criterion.Comparison(VehicleField.COORDINATES_X, Operator.LTE, -5));
    }

    @Test
    void combinesSeveralFilters() {
        VehicleQuery query = parser.parse(List.of("name:like:a", "mileage:lte:50000"), List.of(), 1, 20);

        assertThat(query.criteria()).hasSize(2);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', quoteCharacter = '"', value = {
            "color:eq:red                   | unknown field 'color'",
            "name:contains:a                | unknown operator 'contains'",
            "name:gt:a                      | operator 'gt' does not apply to field 'name'",
            "type:gt:CHOPPER                | operator 'gt' does not apply to field 'type'",
            "id:isnull:true                 | operator 'isnull' does not apply to field 'id'",
            "enginePower:like:1             | operator 'like' does not apply to field 'enginePower'",
            "enginePower:eq:NaN             | 'NaN' is not a number",
            "enginePower:eq:Infinity        | 'Infinity' is not a number",
            "enginePower:eq:1.5f            | '1.5f' is not a number",
            "enginePower:eq:0x10            | '0x10' is not a number",
            "enginePower:eq:1e39            | '1e39' is out of the float range",
            "id:eq:1.5                      | '1.5' is not an integer",
            "id:eq:99999999999              | '99999999999' is out of the int32 range",
            "type:eq:TANK                   | 'TANK' is not one of HELICOPTER, MOTORCYCLE, CHOPPER, SPACESHIP",
            "type:eq:chopper                | 'chopper' is not one of HELICOPTER, MOTORCYCLE, CHOPPER, SPACESHIP",
            "mileage:in:1,,2                | in expects a comma-separated list without empty items",
            "mileage:isnull:yes             | isnull expects true or false",
            "name:eq                        | must have the form field:operator:value",
    })
    void rejectsInvalidFilter(String filter, String message) {
        assertThatThrownBy(() -> parser.parse(List.of(filter), List.of(), 1, 20))
                .isInstanceOfSatisfying(InvalidParameterException.class, e -> {
                    assertThat(e.getParameter()).isEqualTo("filter/0");
                    assertThat(e.getMessage()).isEqualTo(message);
                });
    }

    @Test
    void hintsAtEncodingOfPlusSign() {
        // an unencoded + arrives as a space
        assertThatThrownBy(() -> filter("creationDate:gt:2026-09-10T08:19:00 03:00"))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("%2B");
    }

    @Test
    void pointsAtTheOffendingFilter() {
        assertThatThrownBy(() -> parser.parse(List.of("name:like:a", "name:gt:b"), List.of(), 1, 20))
                .isInstanceOfSatisfying(InvalidParameterException.class,
                        e -> assertThat(e.getParameter()).isEqualTo("filter/1"));
    }

    @Test
    void parsesSortKeysInPriorityOrder() {
        VehicleQuery query = parser.parse(List.of(), List.of("type", "-enginePower"), 2, 10);

        assertThat(query.sort()).containsExactly(
                SortKey.ascending(VehicleField.TYPE),
                SortKey.descending(VehicleField.ENGINE_POWER));
        assertThat(query.page().page()).isEqualTo(2);
        assertThat(query.page().size()).isEqualTo(10);
    }

    @Test
    void rejectsUnknownAndRepeatedSortFields() {
        assertThatThrownBy(() -> parser.parse(List.of(), List.of("-speed"), 1, 20))
                .isInstanceOfSatisfying(InvalidParameterException.class,
                        e -> assertThat(e.getParameter()).isEqualTo("sort/0"));
        assertThatThrownBy(() -> parser.parse(List.of(), List.of("name", "-name"), 1, 20))
                .isInstanceOfSatisfying(InvalidParameterException.class,
                        e -> assertThat(e.getParameter()).isEqualTo("sort/1"));
    }

    @Test
    void treatsMissingParametersAsEmpty() {
        VehicleQuery query = parser.parse(null, null, 1, 20);

        assertThat(query.criteria()).isEmpty();
        assertThat(query.sort()).isEmpty();
    }

    private Criterion filter(String filter) {
        return parser.parse(List.of(filter), List.of(), 1, 20).criteria().get(0);
    }
}
