package com.sutra.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The money arithmetic every quotation total, comparison and grade depends on. */
class MoneyTest {

    @ParameterizedTest(name = "{0} rounds to {1}")
    @CsvSource({"10.005, 10.01", "10.004, 10.00", "-2.345, -2.35", "7, 7.00"})
    void roundUsesHalfUpToTwoPlaces(String value, String expected) {
        assertThat(Money.round(new BigDecimal(value))).isEqualTo(new BigDecimal(expected));
    }

    @Test
    void roundAndNzHandleNull() {
        assertThat(Money.round(null)).isNull();
        assertThat(Money.nz(null)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(Money.nz(new BigDecimal("4.20"))).isEqualByComparingTo("4.20");
    }

    @Test
    void percentOfComputesGstOnALineTotal() {
        // 18% GST on 12,500.00
        assertThat(Money.percentOf(new BigDecimal("12500.00"), new BigDecimal("18")))
                .isEqualByComparingTo("2250.00");
    }

    @Test
    void approximatelyEqualAcceptsSmallRoundingDifferences() {
        assertThat(Money.approximatelyEqual(new BigDecimal("100.00"), new BigDecimal("100.05"))).isTrue();
        // 0.1% relative tolerance on large amounts: 1,000,000 vs 1,000,900
        assertThat(Money.approximatelyEqual(new BigDecimal("1000000"), new BigDecimal("1000900"))).isTrue();
    }

    @Test
    void approximatelyEqualRejectsRealDifferencesAndNulls() {
        // 0.06 is above both the 0.05 absolute and the 0.1% (0.01) relative tolerance at this size
        assertThat(Money.approximatelyEqual(new BigDecimal("10.00"), new BigDecimal("10.06"))).isFalse();
        assertThat(Money.approximatelyEqual(new BigDecimal("1000000"), new BigDecimal("1001500"))).isFalse();
        assertThat(Money.approximatelyEqual(null, BigDecimal.ONE)).isFalse();
        assertThat(Money.approximatelyEqual(BigDecimal.ONE, null)).isFalse();
    }

    @Test
    void percentDifferenceIsRelativeToTheBase() {
        assertThat(Money.percentDifference(new BigDecimal("110"), new BigDecimal("100"))).isEqualByComparingTo("10.00");
        assertThat(Money.percentDifference(new BigDecimal("95"), new BigDecimal("100"))).isEqualByComparingTo("-5.00");
    }

    @Test
    void percentDifferenceIsNullWhenTheBaseIsMissingOrZero() {
        assertThat(Money.percentDifference(BigDecimal.TEN, BigDecimal.ZERO)).isNull();
        assertThat(Money.percentDifference(BigDecimal.TEN, null)).isNull();
        assertThat(Money.percentDifference(null, BigDecimal.TEN)).isNull();
    }
}
