package com.e_commerce.kento_shopping.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class OrderItemResponse {
    private Long id;

    private String productName;
    private Integer quantity;
    private String imageUrl;

    private BigDecimal priceAtPurchase;
    private BigDecimal subTotal;
}
