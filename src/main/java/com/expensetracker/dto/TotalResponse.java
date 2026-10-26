package com.expensetracker.dto;

import java.math.BigDecimal;

public class TotalResponse {

    private BigDecimal totalAmount;
    private long count;

    public TotalResponse() {
    }

    public TotalResponse(BigDecimal totalAmount, long count) {
        this.totalAmount = totalAmount;
        this.count = count;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
