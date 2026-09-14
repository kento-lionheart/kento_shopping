package com.e_commerce.kento_shopping.dto.response;

import com.e_commerce.kento_shopping.enums.FlashSaleStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FlashSaleReconciliationResponse {
    private Long saleId;
    private FlashSaleStatus status;
    private Integer allocatedQty;
    private Integer redisStock;
    private Integer redisInflight;
    private Integer soldQty;
    private Long persistedUnits;
    private boolean consistent;
    private List<String> problems;
}
