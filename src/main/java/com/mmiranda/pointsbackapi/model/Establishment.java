package com.mmiranda.pointsbackapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
public class Establishment {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String phone;
    private Integer valuePerPoint;

    @Column(unique = true)
    private String cnpj;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "group_id")
    private EstablishmentGroup group;

    /** Opt-in: when true (and the establishment is in a group) its clients may be imported by the group's other sharing members. */
    private boolean shareClients;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_mode", nullable = false)
    private RewardMode rewardMode = RewardMode.CATALOG;

    /** Currency value of one point (used by the DISCOUNT and CASHBACK modes). */
    @Column(name = "points_to_currency_rate", nullable = false)
    private BigDecimal pointsToCurrencyRate = new BigDecimal("0.1000");

    /** Largest share of a purchase that points may pay for, in percent (1 to 100). */
    @Column(name = "max_discount_percent", nullable = false)
    private int maxDiscountPercent = 50;

    /** New establishments start on a free trial; existing rows were migrated to ACTIVE. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstablishmentStatus status = EstablishmentStatus.TRIAL;

    /** Free text label of the commercial plan (informational). */
    private String plan;

    @Column(name = "trial_ends_at")
    private LocalDate trialEndsAt;

    public static final int DEFAULT_TRIAL_DAYS = 14;

    @PrePersist
    void applyTrialDefaults() {
        if (status == EstablishmentStatus.TRIAL && trialEndsAt == null) {
            trialEndsAt = LocalDate.now().plusDays(DEFAULT_TRIAL_DAYS);
        }
    }

    /** SUSPENDED when suspended or when the trial has ended; otherwise the stored status. */
    public EstablishmentStatus effectiveStatus(LocalDate today) {
        if (status == EstablishmentStatus.TRIAL && trialEndsAt != null && today.isAfter(trialEndsAt)) {
            return EstablishmentStatus.SUSPENDED;
        }
        return status;
    }

    /** Exposed in the JSON of the establishment list so the admin screen shows what is really enforced today. */
    @JsonProperty("effectiveStatus")
    public EstablishmentStatus currentEffectiveStatus() {
        return effectiveStatus(LocalDate.now());
    }

    public boolean isBlocked(LocalDate today) {
        return effectiveStatus(today) == EstablishmentStatus.SUSPENDED;
    }

    public Establishment() {
    }

    public Establishment(Long id, String name, String email, String phone, Integer valuePerPoint, String cnpj) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.valuePerPoint = valuePerPoint;
        this.cnpj = cnpj;
    }
}

