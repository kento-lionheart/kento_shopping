package com.e_commerce.kento_shopping.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "INVENTORY")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    private Long id;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    @NotNull
    @Min(0)
    private Integer quantity;

    @Version
    private Integer version;
}