package com.spendwise.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Expense {

    private int id;
    private BigDecimal amount;
    private int categoryId;
    private String categoryName;
    private String description;
    private LocalDate expenseDate;

    public Expense() {
    }

    public Expense(BigDecimal amount, int categoryId,
            String description, LocalDate expenseDate) {
        this.amount = amount;
        this.categoryId = categoryId;
        this.description = description;
        this.expenseDate = expenseDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }
}