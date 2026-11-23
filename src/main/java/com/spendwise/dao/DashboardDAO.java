package com.spendwise.dao;

import com.spendwise.config.DatabaseConnection;
import java.sql.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class DashboardDAO {

    public Map<String, Object> getMonthlySummary(
            LocalDate startDate, LocalDate endDate) throws SQLException {

        String sql = "SELECT " +
                "COALESCE(SUM(e.amount), 0) AS total_amount, " +
                "COUNT(*) AS transaction_count, " +
                "(SELECT c.name " +
                " FROM expenses e2 " +
                " JOIN categories c ON e2.category_id = c.id " +
                " WHERE e2.expense_date >= ? AND e2.expense_date < ? " +
                " GROUP BY c.id, c.name " +
                " ORDER BY SUM(e2.amount) DESC " +
                " LIMIT 1) AS top_category " +
                "FROM expenses e " +
                "WHERE e.expense_date >= ? AND e.expense_date < ?";

        Map<String, Object> result = new HashMap<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
            stmt.setDate(3, Date.valueOf(startDate));
            stmt.setDate(4, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal("total_amount");
                    int count = rs.getInt("transaction_count");
                    String topCategory = rs.getString("top_category");

                    BigDecimal average = count > 0
                            ? total.divide(BigDecimal.valueOf(count), 2,
                                    RoundingMode.HALF_UP)
                            : BigDecimal.ZERO.setScale(2);

                    result.put("totalAmount", total);
                    result.put("transactionCount", count);
                    result.put("averageAmount", average);
                    result.put("topCategory", topCategory);
                }
            }
        }

        return result;
    }
}