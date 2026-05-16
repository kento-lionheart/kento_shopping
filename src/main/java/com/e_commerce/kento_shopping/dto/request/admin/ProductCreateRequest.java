package com.e_commerce.kento_shopping.dto.request.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductCreateRequest extends ProductRequest{
    @NotNull(message = "Initial quantity is required")
    @Min(value = 0 , message = "Quantity can not be negative")
    private Integer quantity;
}
