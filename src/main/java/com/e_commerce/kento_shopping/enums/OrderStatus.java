package com.e_commerce.kento_shopping.enums;

public enum OrderStatus {
    PENDING,    // order placed, not yet paid
    PAID,       // payment confirmed
    SHIPPED,    // handed to delivery
    DELIVERED,  // received by customer
    CANCELLED   // cancelled by customer or admin
}
