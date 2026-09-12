package com.e_commerce.kento_shopping.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateTopUpRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "10000", message = "Minimum top-up is 10,000 coins")
    @DecimalMax(value = "50000000", message = "Maximum top-up is 50,000,000 coins")
    private BigDecimal amount;
}
