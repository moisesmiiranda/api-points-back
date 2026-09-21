package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.model.Establishment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RewardCalculatorTest {

    private static Establishment establishment(String rate, int maxPercent) {
        Establishment establishment = new Establishment();
        establishment.setPointsToCurrencyRate(new BigDecimal(rate));
        establishment.setMaxDiscountPercent(maxPercent);
        return establishment;
    }

    @Test
    void pointsAreFlooredPerFullValuePerPoint() {
        assertEquals(10, RewardCalculator.pointsFor(10, new BigDecimal("100.00")));
        assertEquals(9, RewardCalculator.pointsFor(10, new BigDecimal("99.99")));
        assertEquals(0, RewardCalculator.pointsFor(10, new BigDecimal("9.99")));
        assertEquals(0, RewardCalculator.pointsFor(10, BigDecimal.ZERO));
    }

    @Test
    void discountIsRoundedDownToTheCent() {
        assertEquals(new BigDecimal("20.00"), RewardCalculator.discountFor(establishment("0.1000", 50), 200));
        assertEquals(new BigDecimal("0.33"), RewardCalculator.discountFor(establishment("0.1111", 50), 3));
    }

    @Test
    void maxDiscountIsAPercentageOfTheAmountRoundedDown() {
        assertEquals(new BigDecimal("50.00"), RewardCalculator.maxDiscount(establishment("0.1", 50), new BigDecimal("100.00")));
        assertEquals(new BigDecimal("3.33"), RewardCalculator.maxDiscount(establishment("0.1", 33), new BigDecimal("10.10")));
        assertEquals(new BigDecimal("99.99"), RewardCalculator.maxDiscount(establishment("0.1", 100), new BigDecimal("99.99")));
    }

    @Test
    void maxPointsIsTheSmallerOfBalanceAndWhatTheCapAllows() {
        Establishment establishment = establishment("0.1000", 50);
        assertEquals(500, RewardCalculator.maxPoints(establishment, new BigDecimal("100.00"), 10_000));
        assertEquals(120, RewardCalculator.maxPoints(establishment, new BigDecimal("100.00"), 120));
        assertEquals(0, RewardCalculator.maxPoints(establishment, new BigDecimal("0.05"), 120));
    }
}
