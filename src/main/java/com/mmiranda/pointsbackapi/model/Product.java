package com.mmiranda.pointsbackapi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private BigDecimal price;
    private Integer pointForSell;

    @ManyToOne
    @JoinColumn(name = "establishment_id")
    private Establishment establishment;
}
