package com.e_commerce.kento_shopping.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FlashSalePurchaseRequest {
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "You may purchase between 1 and 3 units per request")
    @Max(value = 3, message = "You may purchase between 1 and 3 units per request")
    private Integer quantity;
}
