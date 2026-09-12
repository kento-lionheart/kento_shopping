package com.e_commerce.kento_shopping.enums;

public enum CoinTxType {
    TOP_UP,      // + credited when an admin approves a top-up request
    PURCHASE,    // - debited when an order is paid
    REFUND,      // + credited when a paid order is cancelled (not yet reachable)
    ADJUSTMENT   // +/- manual correction by an admin
}
