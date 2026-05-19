package com.e_commerce.kento_shopping.dto.request.admin;

import com.e_commerce.kento_shopping.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateOrderStatusRequest {
    @NotNull(message = "Status is required")
    private OrderStatus status;
}
