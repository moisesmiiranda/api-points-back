package com.mmiranda.pointsbackapi.model;

/**
 * How an establishment lets clients spend points. CATALOG: exchange for gifts/coupons.
 * DISCOUNT: points take money off a purchase. CASHBACK: the same mechanism, shown to the client
 * as a money balance to use on future purchases (and it may cover the whole purchase).
 */
public enum RewardMode {
    CATALOG, DISCOUNT, CASHBACK
}
