package com.e_commerce.kento_shopping.dto.response;

import com.e_commerce.kento_shopping.enums.FlashSaleStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class FlashSaleResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private BigDecimal originalPrice;
    private String name;
    private BigDecimal salePrice;
    private Integer allocatedQty;
    private Integer soldQty;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private FlashSaleStatus status;
    private LocalDateTime createdAt;
}
