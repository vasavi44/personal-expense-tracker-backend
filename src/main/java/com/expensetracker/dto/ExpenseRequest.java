package com.expensetracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpenseRequest {

    @NotBlank(message = "Expense title cannot be empty or contain only whitespace")
    @Size(min = 1, max = 100, message = "Expense title must be between 1 and 100 characters")
    private String title;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be a valid number greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Category is required")
    @Pattern(
        regexp = "^(Food|Travel|Shopping|Bills|Entertainment|Health|Education|Other)$",
        message = "Category must be one of: Food, Travel, Shopping, Bills, Entertainment, Health, Education, Other"
    )
    private String category;

    @NotNull(message = "Expense date is required")
    private LocalDate date;

    public ExpenseRequest() {
    }

    public ExpenseRequest(String title, BigDecimal amount, String category, LocalDate date) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title != null ? title.trim() : null;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category != null ? category.trim() : null;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
