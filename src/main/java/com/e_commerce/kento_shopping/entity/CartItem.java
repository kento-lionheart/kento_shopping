package com.e_commerce.kento_shopping.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "CART_ITEM")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @EmbeddedId
    private Id id;

    @ManyToOne(optional = false)
    @MapsId("cartId")
    @JoinColumn(name = "CART_ID")
    private Cart cart;

    @ManyToOne(optional = false)
    @MapsId("productId")
    @JoinColumn(name = "PRODUCT_ID")
    private Product product;

    @Column(nullable = false)
    @NotNull
    @Min(1)
    private Integer quantity;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Embeddable
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {

        @Column(name = "CART_ID")
        private Long cartId;

        @Column(name = "PRODUCT_ID")
        private Long productId;
    }
}
