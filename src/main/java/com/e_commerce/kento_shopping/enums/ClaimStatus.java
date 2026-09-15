package com.e_commerce.kento_shopping.enums;

public enum ClaimStatus {
    PROCESSING, // stock claimed in Redis, order not yet persisted
    PAID,       // order written and coins debited
    FAILED      // rejected by the consumer, stock returned
}
