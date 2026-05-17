package com.e_commerce.kento_shopping.dto.response;

import com.e_commerce.kento_shopping.enums.OrderStatus;
import com.e_commerce.kento_shopping.enums.PaymentMethod;
import com.e_commerce.kento_shopping.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class OrderResponse {
    private Long orderId;
    private OrderStatus orderStatus;

    private BigDecimal subTotal;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;

    private String shipRecipientName;
    private String shipPhone;
    private String shipStreet;
    private String shipWard;
    private String shipDistrict;
    private String shipCity;
    private String shipPostalCode;

    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private String transactionId;

    private List<OrderItemResponse> items;

    private LocalDateTime createdAt;
}
