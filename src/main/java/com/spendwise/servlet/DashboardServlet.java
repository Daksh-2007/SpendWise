package com.spendwise.servlet;

import com.spendwise.dao.DashboardDAO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.sql.SQLException;
import java.util.Map;

@WebServlet("/api/dashboard")
public class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String month = request.getParameter("month");

        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Month is required in YYYY-MM format\"}");
            return;
        }

        try {
            YearMonth yearMonth = YearMonth.parse(month);

            Map<String, Object> data = new DashboardDAO().getMonthlySummary(
                    yearMonth.atDay(1),
                    yearMonth.plusMonths(1).atDay(1));

            String topCategory = (String) data.get("topCategory");

            String json = "{"
                    + "\"month\":\"" + month + "\","
                    + "\"totalAmount\":" + data.get("totalAmount") + ","
                    + "\"transactionCount\":" + data.get("transactionCount") + ","
                    + "\"averageAmount\":" + data.get("averageAmount") + ","
                    + "\"topCategory\":"
                    + (topCategory == null ? "null" : "\"" + topCategory + "\"")
                    + "}";

            response.getWriter().write(json);

        } catch (DateTimeParseException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Invalid month\"}");

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Database error\"}");
            e.printStackTrace();
        }
    }
}