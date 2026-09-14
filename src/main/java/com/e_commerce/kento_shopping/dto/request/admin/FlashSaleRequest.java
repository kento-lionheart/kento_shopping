package com.e_commerce.kento_shopping.dto.request.admin;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class FlashSaleRequest {
    @NotNull(message = "Product is required")
    private Long productId;

    @NotBlank(message = "Sale name is required")
    private String name;

    @NotNull(message = "Sale price is required")
    @Positive(message = "Sale price must be greater than 0")
    private BigDecimal salePrice;

    @NotNull(message = "Allocated quantity is required")
    @Positive(message = "Allocated quantity must be greater than 0")
    private Integer allocatedQty;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startAt;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    private LocalDateTime endAt;
}
