package com.e_commerce.kento_shopping.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockRequest {
    @NotNull(message = "Quantity must not be empty")
    @PositiveOrZero(message = "Quantity can not be negative")
    private Integer quantity;
}
