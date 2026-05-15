package com.e_commerce.kento_shopping.dto.response;

import com.e_commerce.kento_shopping.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String categoryName;
    private Integer quantity;
    private String stockStatus;
}
