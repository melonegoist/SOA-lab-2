package org.itmo.vehicle.domain.query;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VehicleQueryTest {

    private static final PageRequest FIRST_PAGE = new PageRequest(1, 20);

    @Test
    void sortsByIdWhenNothingElseIsRequested() {
        var query = new VehicleQuery(List.of(), List.of(), FIRST_PAGE);

        assertThat(query.effectiveSort()).containsExactly(SortKey.ascending(VehicleField.ID));
    }

    @Test
    void appendsIdAsLastTieBreaker() {
        var query = new VehicleQuery(List.of(),
                List.of(SortKey.ascending(VehicleField.TYPE), SortKey.descending(VehicleField.ENGINE_POWER)),
                FIRST_PAGE);

        assertThat(query.effectiveSort()).containsExactly(
                SortKey.ascending(VehicleField.TYPE),
                SortKey.descending(VehicleField.ENGINE_POWER),
                SortKey.ascending(VehicleField.ID));
    }

    @Test
    void keepsRequestedIdKey() {
        var query = new VehicleQuery(List.of(), List.of(SortKey.descending(VehicleField.ID)), FIRST_PAGE);

        assertThat(query.effectiveSort()).containsExactly(SortKey.descending(VehicleField.ID));
    }

    @Test
    void rejectsDuplicateSortField() {
        assertThatThrownBy(() -> new VehicleQuery(List.of(),
                List.of(SortKey.ascending(VehicleField.NAME), SortKey.descending(VehicleField.NAME)), FIRST_PAGE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void countsPages() {
        assertThat(new Page<>(List.of(), 1, 20, 0).totalPages()).isZero();
        assertThat(new Page<>(List.of(), 1, 20, 20).totalPages()).isEqualTo(1);
        assertThat(new Page<>(List.of(), 1, 20, 21).totalPages()).isEqualTo(2);
    }

    @Test
    void computesOffsetWithoutOverflow() {
        assertThat(new PageRequest(Integer.MAX_VALUE, 100).offset()).isEqualTo((Integer.MAX_VALUE - 1L) * 100);
    }

    @Test
    void appliesOperatorsAsTheContractSays() {
        assertThat(Operator.GT.isApplicableTo(VehicleField.CREATION_DATE)).isTrue();
        assertThat(Operator.GT.isApplicableTo(VehicleField.NAME)).isFalse();
        assertThat(Operator.GT.isApplicableTo(VehicleField.TYPE)).isFalse();
        assertThat(Operator.LIKE.isApplicableTo(VehicleField.NAME)).isTrue();
        assertThat(Operator.LIKE.isApplicableTo(VehicleField.TYPE)).isFalse();
        assertThat(Operator.IS_NULL.isApplicableTo(VehicleField.MILEAGE)).isTrue();
        assertThat(Operator.IS_NULL.isApplicableTo(VehicleField.ID)).isFalse();
        assertThat(Operator.IN.isApplicableTo(VehicleField.FUEL_TYPE)).isTrue();
    }

    @Test
    void criterionRejectsValueOfWrongType() {
        assertThatThrownBy(() -> new Criterion.Comparison(VehicleField.ENGINE_POWER, Operator.GT, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
