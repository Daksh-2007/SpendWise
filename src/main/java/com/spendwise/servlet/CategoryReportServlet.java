package com.spendwise.servlet;

import com.spendwise.dao.ReportDAO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@WebServlet("/api/reports/categories")
public class CategoryReportServlet extends HttpServlet {

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

            java.sql.Date startDate = java.sql.Date.valueOf(yearMonth.atDay(1));

            java.sql.Date endDate = java.sql.Date.valueOf(yearMonth.plusMonths(1).atDay(1));

            List<Map<String, Object>> reports = new ReportDAO().getCategoryReport(startDate, endDate);

            BigDecimal grandTotal = BigDecimal.ZERO;

            for (Map<String, Object> item : reports) {
                grandTotal = grandTotal.add(
                        (BigDecimal) item.get("totalAmount"));
            }

            StringBuilder json = new StringBuilder();
            json.append("{\"month\":\"").append(month).append("\",");
            json.append("\"totalAmount\":").append(grandTotal).append(",");
            json.append("\"categories\":[");

            for (int i = 0; i < reports.size(); i++) {
                Map<String, Object> item = reports.get(i);

                BigDecimal amount = (BigDecimal) item.get("totalAmount");

                BigDecimal percentage = grandTotal.compareTo(BigDecimal.ZERO) > 0
                        ? amount.multiply(BigDecimal.valueOf(100))
                                .divide(grandTotal, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO.setScale(2);

                if (i > 0) {
                    json.append(",");
                }

                json.append("{")
                        .append("\"categoryId\":").append(item.get("categoryId")).append(",")
                        .append("\"categoryName\":\"")
                        .append(escapeJson((String) item.get("categoryName"))).append("\",")
                        .append("\"totalAmount\":").append(amount).append(",")
                        .append("\"transactionCount\":").append(item.get("transactionCount")).append(",")
                        .append("\"percentage\":").append(percentage)
                        .append("}");
            }

            json.append("]}");

            response.getWriter().write(json.toString());

        } catch (java.time.format.DateTimeParseException e) {
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

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}