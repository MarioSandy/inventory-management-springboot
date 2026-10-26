package com.example.inventory_api_springboot.enums;

public enum InventoryLogTypeEnum {
    RESTOCK {
        @Override
        public void validate(int quantity, int reservedQuantity) {
            if (
                quantity <= 0
                || reservedQuantity != 0
            ) throw invalid(
                "Quantity must be positive & Reserved quantity must be 0 for restocks."
            );
        }
    },
    DAMAGED {
        @Override
        public void validate(int quantity, int reservedQuantity) {
            if (
                quantity >= 0
                || reservedQuantity != 0
            ) throw invalid(
                "Quantity must be negative & Reserved quantity must be 0 for damage reports."
            );
        }
    },
    ORDER_RESERVED {
        @Override
        public void validate(int quantity, int reservedQuantity) {
            if (
                quantity != 0
                || reservedQuantity <= 0
            ) throw invalid(
                "Quantity must be 0 & Reserved quantity must be positive when creating an order."
            );
        }
    },
    ORDER_SHIPPED {
        @Override
        public void validate(int quantity, int reservedQuantity) {
            if (
                quantity >= 0
                || reservedQuantity >= 0
            ) throw invalid(
                "Quantity & Reserved quantity must be negative when shipping an order."
            );
        }
    },
    STOCK_OPNAME {
        @Override
        public void validate(int quantity, int reservedQuantity) {
            if (
                quantity == 0
                || reservedQuantity != 0
            ) throw invalid(
                "Quantity should not be 0 and reserved quantity must be 0 when stock opname."
            );
        }
    },
    RESERVATION_RELEASE {
        @Override
        public void validate(int quantity, int reservedQuantity) {
            if (
                quantity != 0
                || reservedQuantity >= 0
            ) throw invalid(
                "Quantity should be 0 and reserved quantity must be positive when reservation release."
            );
        }
    };

    public abstract void validate(int quantity, int reservedQuantity);

    protected RuntimeException invalid(String message) {
        return new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, message
        );
    }
}

