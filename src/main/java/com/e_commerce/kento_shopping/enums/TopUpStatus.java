package com.e_commerce.kento_shopping.enums;

public enum TopUpStatus {
    PENDING,    // awaiting admin review
    APPROVED,   // coins credited
    REJECTED,   // closed by an admin, no coins
    CANCELLED   // withdrawn by the requester
}
