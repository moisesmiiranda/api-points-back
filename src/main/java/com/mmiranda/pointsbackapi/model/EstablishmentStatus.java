package com.mmiranda.pointsbackapi.model;

/**
 * Commercial state of an establishment. TRIAL ends on {@code trialEndsAt} (after that it behaves as
 * SUSPENDED); SUSPENDED blocks its users (for example for non-payment) until an admin reactivates it.
 */
public enum EstablishmentStatus {
    TRIAL, ACTIVE, SUSPENDED
}
