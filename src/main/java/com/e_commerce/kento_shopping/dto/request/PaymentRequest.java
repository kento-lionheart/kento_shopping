package com.e_commerce.kento_shopping.dto.request;

import com.e_commerce.kento_shopping.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class PaymentRequest {
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}
