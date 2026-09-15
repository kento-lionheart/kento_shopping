package com.e_commerce.kento_shopping.enums;

public enum FlashSaleStatus {
    SCHEDULED,  // created, inventory untouched, editable
    ACTIVE,     // stock carved out of inventory and loaded into Redis
    PAUSED,     // circuit breaker tripped, claims refused
    CLOSING,    // claims stopped, waiting for in-flight claims to settle
    ENDED,      // closed, unsold remainder returned to inventory
    CANCELLED   // withdrawn before activation
}
