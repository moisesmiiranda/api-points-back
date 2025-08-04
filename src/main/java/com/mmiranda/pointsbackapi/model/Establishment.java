package com.mmiranda.pointsbackapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

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

    public Establishment( String name, String email, String phone, Integer valuePerPoint, String cnpj) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.valuePerPoint = valuePerPoint;
        this.cnpj = cnpj;
    }

    public Establishment() {
    }
}

