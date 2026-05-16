package com.e_commerce.kento_shopping.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CartItemRequest {
    @NotNull(message = "Product is required")
    private Long productId;

    @NotNull(message =  "Quantity is required")
    @Min(value = 1, message = "Quantity must be greater than 1")
    private Integer quantity;
}
