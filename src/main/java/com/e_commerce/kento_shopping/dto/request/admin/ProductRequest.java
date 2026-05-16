package com.e_commerce.kento_shopping.dto.request.admin;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequest {
    @NotBlank(message = "Product's name is required")
    private String name;

    private String description;

    @NotNull(message = "Product's price is required")
    @Positive(message = "Price must be greater than 0")
    private BigDecimal price;

    private String imageUrl;

    @NotNull(message = "Category is required")
    private Long categoryId;

}
