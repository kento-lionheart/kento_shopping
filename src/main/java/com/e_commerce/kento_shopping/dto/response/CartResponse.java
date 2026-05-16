package com.e_commerce.kento_shopping.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class CartResponse {
    private List<CartItemResponse> items;
    private BigDecimal total;
    private int itemCount;
}
