package com.e_commerce.kento_shopping.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectTopUpRequest {
    @NotBlank(message = "A note is required when rejecting")
    private String note;
}
