package com.e_commerce.kento_shopping.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class CartItemResponse {
    private Long productId;
    private String productName;
    private BigDecimal price;
    private BigDecimal subTotal;
    private String imageUrl;
    private Integer quantity;
    private String stockStatus;
}
