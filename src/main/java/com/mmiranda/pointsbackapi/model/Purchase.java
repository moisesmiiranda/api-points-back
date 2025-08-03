package com.mmiranda.pointsbackapi.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
public class Purchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Client client;

    @ManyToOne
    private Product product;

    // talvez não seja necessário a quantidade, porém, vamos deixar aqui por enquanto
    private Integer quantity;
    private LocalDateTime purchaseDate;
}

