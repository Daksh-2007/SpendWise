package com.spendwise.dao;

import com.spendwise.config.DatabaseConnection;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;

public class ReportDAO {

    public List<Map<String, Object>> getCategoryReport(
            java.sql.Date startDate, java.sql.Date endDate) throws SQLException {

        String sql = "SELECT c.id, c.name, " +
                "COALESCE(SUM(e.amount), 0) AS total_amount, " +
                "COUNT(e.id) AS transaction_count " +
                "FROM categories c " +
                "LEFT JOIN expenses e ON e.category_id = c.id " +
                "AND e.expense_date >= ? AND e.expense_date < ? " +
                "GROUP BY c.id, c.name " +
                "ORDER BY total_amount DESC, c.id";

        List<Map<String, Object>> reports = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, startDate);
            stmt.setDate(2, endDate);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();

                    item.put("categoryId", rs.getInt("id"));
                    item.put("categoryName", rs.getString("name"));
                    item.put("totalAmount", rs.getBigDecimal("total_amount"));
                    item.put("transactionCount", rs.getInt("transaction_count"));

                    reports.add(item);
                }
            }
        }

        return reports;
    }
}