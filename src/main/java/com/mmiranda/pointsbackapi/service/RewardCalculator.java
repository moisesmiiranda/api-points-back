package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.model.Establishment;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The arithmetic of points. Every result is rounded DOWN (in the establishment's favour), so
 * "floor" is the single rounding rule: points earned, discount value and redeemable points.
 */
final class RewardCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private RewardCalculator() {
    }

    /** Points granted for paying {@code paid}: one point per full {@code valuePerPoint}. */
    static int pointsFor(int valuePerPoint, BigDecimal paid) {
        return paid.divide(BigDecimal.valueOf(valuePerPoint), 0, RoundingMode.DOWN).intValue();
    }

    /** Money value of {@code points} at the establishment's rate, to the cent. */
    static BigDecimal discountFor(Establishment establishment, int points) {
        return establishment.getPointsToCurrencyRate()
                .multiply(BigDecimal.valueOf(points))
                .setScale(2, RoundingMode.DOWN);
    }

    /** The most a purchase of {@code amount} may be discounted by. */
    static BigDecimal maxDiscount(Establishment establishment, BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(establishment.getMaxDiscountPercent()))
                .divide(HUNDRED, 2, RoundingMode.DOWN);
    }

    /** The most points a client with {@code balance} may spend on a purchase of {@code amount}. */
    static int maxPoints(Establishment establishment, BigDecimal amount, int balance) {
        int allowedByCap = maxDiscount(establishment, amount)
                .divide(establishment.getPointsToCurrencyRate(), 0, RoundingMode.DOWN)
                .intValue();
        return Math.min(balance, allowedByCap);
    }
}
