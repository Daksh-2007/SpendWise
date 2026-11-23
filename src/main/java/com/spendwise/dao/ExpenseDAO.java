package com.spendwise.dao;

import com.spendwise.config.DatabaseConnection;
import com.spendwise.model.Expense;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import com.spendwise.config.DatabaseConnection;

public class ExpenseDAO {

    // ADD EXPENSE
    public boolean addExpense(Expense expense) throws SQLException {

        String sql = """
                INSERT INTO expenses
                (amount, category_id, description, expense_date)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBigDecimal(1, expense.getAmount());
            ps.setInt(2, expense.getCategoryId());
            ps.setString(3, expense.getDescription());
            ps.setDate(4, Date.valueOf(expense.getExpenseDate()));

            return ps.executeUpdate() == 1;
        }
    }

    // GET EXPENSES WITH FILTERS
    public List<Expense> getExpenses(
            String month,
            Integer categoryId,
            String search,
            LocalDate date,
            String sort) throws SQLException {

        List<Expense> expenses = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
                SELECT e.id, e.amount, e.category_id,
                       c.name AS category_name,
                       e.description, e.expense_date
                FROM expenses e
                JOIN categories c ON e.category_id = c.id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        if (month != null && !month.isBlank()) {
            sql.append(" AND e.expense_date >= ? AND e.expense_date < ?");
            LocalDate start = java.time.YearMonth.parse(month).atDay(1);
            params.add(Date.valueOf(start));
            params.add(Date.valueOf(start.plusMonths(1)));
        }

        if (categoryId != null) {
            sql.append(" AND e.category_id = ?");
            params.add(categoryId);
        }

        if (search != null && !search.isBlank()) {
            sql.append(" AND e.description LIKE ?");
            params.add("%" + search.trim() + "%");
        }

        if (date != null) {
            sql.append(" AND e.expense_date = ?");
            params.add(Date.valueOf(date));
        }

        if ("asc".equalsIgnoreCase(sort)) {
            sql.append(" ORDER BY e.amount ASC");
        } else if ("desc".equalsIgnoreCase(sort)) {
            sql.append(" ORDER BY e.amount DESC");
        } else {
            sql.append(" ORDER BY e.expense_date DESC, e.id DESC");
        }

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Expense expense = new Expense();

                    expense.setId(rs.getInt("id"));
                    expense.setAmount(rs.getBigDecimal("amount"));
                    expense.setCategoryId(rs.getInt("category_id"));
                    expense.setCategoryName(rs.getString("category_name"));
                    expense.setDescription(rs.getString("description"));
                    expense.setExpenseDate(
                            rs.getDate("expense_date").toLocalDate());

                    expenses.add(expense);
                }
            }
        }

        return expenses;
    }

    // DELETE EXPENSE
    public boolean deleteExpense(int id) throws SQLException {
    String sql = "DELETE FROM expenses WHERE id = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setInt(1, id);

        return stmt.executeUpdate() > 0;
    }
}

    // EDIT EXPENSE
    public boolean updateExpense(int id, Expense expense)
            throws SQLException {

        String sql = """
                UPDATE expenses
                SET amount = ?, category_id = ?,
                    description = ?, expense_date = ?
                WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBigDecimal(1, expense.getAmount());
            ps.setInt(2, expense.getCategoryId());
            ps.setString(3, expense.getDescription());
            ps.setDate(4, Date.valueOf(expense.getExpenseDate()));
            ps.setInt(5, id);

            return ps.executeUpdate() == 1;
        }
    }
}
