package com.e_commerce.kento_shopping.enums;

import lombok.Getter;

/**
 * The closed set of permissions. A permission only exists because a
 * {@code @PreAuthorize} check refers to it, so this list is defined by the
 * source and seeded from it. Roles, by contrast, are rows and may be composed
 * at runtime.
 */
@Getter
public enum PermissionName {

    PRODUCT_CREATE("Create a product"),
    PRODUCT_UPDATE("Edit a product's details"),
    PRODUCT_DELETE("Delete a product"),
    INVENTORY_UPDATE("Adjust stock levels"),
    CATEGORY_MANAGE("Create, rename and delete categories"),

    ORDER_READ_ALL("View orders belonging to any customer"),
    ORDER_UPDATE_STATUS("Move an order through its lifecycle"),
    ORDER_CANCEL_ANY("Cancel another customer's order, restocking and refunding"),

    USER_READ("View user accounts"),
    USER_UPDATE("Edit or deactivate a user account"),
    ROLE_ASSIGN("Grant and revoke existing roles on a user"),
    ROLE_MANAGE("Create roles and edit their permissions"),

    TOPUP_READ_ALL("View coin top-up requests from any customer"),
    TOPUP_APPROVE("Approve or reject a top-up request, creating coins"),
    WALLET_READ_ALL("Inspect any customer's wallet and ledger"),
    WALLET_ADJUST("Manually correct a wallet balance"),

    FLASHSALE_READ_ALL("View scheduled flash sales before they are public"),
    FLASHSALE_CREATE("Create a flash sale"),
    FLASHSALE_UPDATE("Edit a scheduled flash sale"),
    FLASHSALE_DELETE("Delete a flash sale record"),
    FLASHSALE_ACTIVATE("Manually start or stop a live flash sale");

    private final String description;

    PermissionName(String description) {
        this.description = description;
    }
}
