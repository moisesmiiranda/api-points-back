package com.mmiranda.pointsbackapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@AllArgsConstructor
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

    public Establishment() {
    }
}

