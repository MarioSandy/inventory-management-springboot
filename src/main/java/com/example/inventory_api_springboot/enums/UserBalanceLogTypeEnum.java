package com.example.inventory_api_springboot.enums;

import java.math.BigDecimal;

public enum UserBalanceLogTypeEnum {
    DEPOSIT {
        @Override
        public void validate(BigDecimal balance, BigDecimal reservedBalance) {
            if (
                balance.signum() <= 0
                || reservedBalance.signum() != 0
            ) throw invalid(
                "Balance must be positive & Reserved balance must be 0 for deposit."
            );
        }
    },
    WITHDRAW {
        @Override
        public void validate(BigDecimal balance, BigDecimal reservedBalance) {
            if (
                balance.signum() >= 0
                || reservedBalance.signum() != 0
            ) throw invalid(
                "Quantity must be negative & Reserved quantity must be 0 for damage reports."
            );
        }
    },
    ORDER_RESERVED {
        @Override
        public void validate(BigDecimal balance, BigDecimal reservedBalance) {
            if (
                balance.signum() != 0
                || reservedBalance.signum() <= 0
            ) throw invalid(
                "Balance must be 0 & Reserved balance must be positive when creating an order."
            );
        }
    },
    ORDER_CANCELLED {
        @Override
        public void validate(BigDecimal balance, BigDecimal reservedBalance) {
            if (
                balance.signum() != 0
                || reservedBalance.signum() >= 0
            ) throw invalid(
                "Balance must be 0 & Reserved balance must be negative when canceling an order."
            );
        }
    },
    ORDER_SHIPPED {
        @Override
        public void validate(BigDecimal balance, BigDecimal reservedBalance) {
            if (
                balance.signum() >= 0
                || reservedBalance.signum() >= 0
            ) throw invalid(
                "Balance & Reserved balance must be negative when shipping an order."
            );
        }
    },
    REFUND_STOCK_SHORTAGE {
        @Override
        public void validate(BigDecimal balance, BigDecimal reservedBalance) {
            if (
                balance.signum() != 0
                || reservedBalance.signum() >= 0
            ) throw invalid(
                "Balance must be 0 & Reserved balance must be negative when refund because stock shortage"
            );
        }
    };

    // Abstract method that each enum variant must implement
    public abstract void validate(BigDecimal balance, BigDecimal reservedBalance);

    // Helper to throw a standardized client-facing error
    protected RuntimeException invalid(String message) {
        return new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, message
        );
    }
}
