package com.spendwise.dao;

import com.spendwise.config.DatabaseConnection;
import com.spendwise.model.Category;
import java.sql.*;
import java.util.*;

public class CategoryDAO {

    public List<Category> getAllCategories() throws SQLException {
        List<Category> categories = new ArrayList<>();

        String sql = "SELECT id, name FROM categories ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                categories.add(
                        new Category(rs.getInt("id"), rs.getString("name")));
            }
        }

        return categories;
    }
}