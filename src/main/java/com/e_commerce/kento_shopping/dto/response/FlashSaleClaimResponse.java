package com.e_commerce.kento_shopping.dto.response;

import com.e_commerce.kento_shopping.enums.ClaimStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FlashSaleClaimResponse {
    private String claimId;
    private Long saleId;
    private Integer quantity;
    private ClaimStatus status;
    private String message;
    private Long orderId;
}
