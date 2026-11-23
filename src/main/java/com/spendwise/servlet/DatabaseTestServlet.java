
package com.spendwise.servlet;

import com.spendwise.config.DatabaseConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/api/db-check")
public class DatabaseTestServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            response.getWriter().write(
                "{\"success\":true,\"message\":\"SpendWise database connected successfully\"}"
            );

        } catch (SQLException e) {

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            response.getWriter().write(
                "{\"success\":false,\"message\":\"Database connection failed\"}"
            );

            getServletContext().log("Database connection error", e);
        }
    }
}
