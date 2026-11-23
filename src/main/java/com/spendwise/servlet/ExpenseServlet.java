package com.spendwise.servlet;

import com.spendwise.dao.ExpenseDAO;
import com.spendwise.model.Expense;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@WebServlet("/api/expenses")
public class ExpenseServlet extends HttpServlet {

    private final ExpenseDAO expenseDAO = new ExpenseDAO();

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            Expense expense = readExpense(request);

            if (!isValid(expense)) {
                response.setStatus(400);
                response.getWriter().write(
                        "{\"success\":false,\"message\":\"Invalid expense details\"}");
                return;
            }

            if (expenseDAO.addExpense(expense)) {
                response.setStatus(201);
                response.getWriter().write(
                        "{\"success\":true,\"message\":\"Expense added successfully\"}");
            }

        } catch (IllegalArgumentException e) {
            response.setStatus(400);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Invalid input format\"}");
        } catch (SQLException e) {
            getServletContext().log("Expense insert error", e);
            response.setStatus(500);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Unable to save expense\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            String month = request.getParameter("month");
            String categoryParam = request.getParameter("categoryId");
            String search = request.getParameter("search");
            String dateParam = request.getParameter("date");
            String sort = request.getParameter("sort");

            Integer categoryId = null;
            LocalDate date = null;

            if (categoryParam != null && !categoryParam.isBlank()) {
                categoryId = Integer.parseInt(categoryParam);
                if (categoryId <= 0)
                    throw new IllegalArgumentException();
            }

            if (month != null && !month.isBlank()) {
                YearMonth.parse(month);
            }

            if (dateParam != null && !dateParam.isBlank()) {
                date = LocalDate.parse(
                        dateParam,
                        DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            }

            if (sort != null && !sort.isBlank()
                    && !sort.equalsIgnoreCase("asc")
                    && !sort.equalsIgnoreCase("desc")) {
                throw new IllegalArgumentException();
            }

            List<Expense> expenses = expenseDAO.getExpenses(
                    month, categoryId, search, date, sort);

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < expenses.size(); i++) {
                Expense e = expenses.get(i);

                if (i > 0)
                    json.append(",");

                json.append("{")
                        .append("\"id\":").append(e.getId()).append(",")
                        .append("\"amount\":").append(e.getAmount()).append(",")
                        .append("\"categoryId\":").append(e.getCategoryId()).append(",")
                        .append("\"categoryName\":\"")
                        .append(escapeJson(e.getCategoryName())).append("\",")
                        .append("\"description\":\"")
                        .append(escapeJson(e.getDescription())).append("\",")
                        .append("\"expenseDate\":\"")
                        .append(e.getExpenseDate()).append("\"")
                        .append("}");
            }

            json.append("]");
            response.getWriter().write(json.toString());

        } catch (IllegalArgumentException | DateTimeParseException e) {
            response.setStatus(400);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Invalid filter value. Use month YYYY-MM and date DD-MM-YYYY.\"}");
        } catch (SQLException e) {
            getServletContext().log("Expense retrieval error", e);
            response.setStatus(500);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Unable to retrieve expenses\"}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            Map<String, String> form = new HashMap<>();

            String body = request.getReader()
                    .lines()
                    .collect(java.util.stream.Collectors.joining());

            for (String pair : body.split("&")) {
                String[] parts = pair.split("=", 2);

                String key = URLDecoder.decode(
                        parts[0], StandardCharsets.UTF_8);

                String value = parts.length > 1
                        ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                        : "";

                form.put(key, value);
            }

            int id = Integer.parseInt(form.get("id"));

            Expense expense = new Expense(
                    new BigDecimal(form.get("amount")),
                    Integer.parseInt(form.get("categoryId")),
                    form.get("description"),
                    LocalDate.parse(form.get("expenseDate")));

            if (id <= 0 || !isValid(expense)) {
                response.setStatus(400);
                response.getWriter().write(
                        "{\"success\":false,\"message\":\"Invalid expense details\"}");
                return;
            }

            boolean updated = expenseDAO.updateExpense(id, expense);

            if (updated) {
                response.getWriter().write(
                        "{\"success\":true,\"message\":\"Expense updated successfully\"}");
            } else {
                response.setStatus(404);
                response.getWriter().write(
                        "{\"success\":false,\"message\":\"Expense not found\"}");
            }

        } catch (IllegalArgumentException | NullPointerException e) {
            response.setStatus(400);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Invalid input format\"}");
        } catch (SQLException e) {
            getServletContext().log("Expense update error", e);
            response.setStatus(500);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Unable to update expense\"}");
        }
    }

    private Expense readExpense(HttpServletRequest request) {
        BigDecimal amount = new BigDecimal(request.getParameter("amount"));
        int categoryId = Integer.parseInt(request.getParameter("categoryId"));
        String description = request.getParameter("description");
        LocalDate date = LocalDate.parse(request.getParameter("expenseDate"));

        return new Expense(amount, categoryId, description, date);
    }

    private boolean isValid(Expense expense) {
        return expense.getAmount() != null
                && expense.getAmount().compareTo(BigDecimal.ZERO) > 0
                && expense.getAmount().scale() <= 2
                && expense.getCategoryId() > 0
                && expense.getDescription() != null
                && !expense.getDescription().isBlank()
                && expense.getDescription().length() <= 255;
    }

    private String escapeJson(String value) {
        if (value == null)
            return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    @Override
    protected void doDelete(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            String idParam = request.getParameter("id");

            if (idParam == null || idParam.isBlank()) {
                response.setStatus(400);
                response.getWriter().write(
                        "{\"success\":false,\"message\":\"Expense ID is required\"}");
                return;
            }

            int id = Integer.parseInt(idParam);

            if (id <= 0) {
                response.setStatus(400);
                response.getWriter().write(
                        "{\"success\":false,\"message\":\"Invalid expense ID\"}");
                return;
            }

            boolean deleted = expenseDAO.deleteExpense(id);

            if (deleted) {
                response.getWriter().write(
                        "{\"success\":true,\"message\":\"Expense deleted successfully\"}");
            } else {
                response.setStatus(404);
                response.getWriter().write(
                        "{\"success\":false,\"message\":\"Expense not found\"}");
            }

        } catch (NumberFormatException e) {
            response.setStatus(400);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Invalid expense ID\"}");
        } catch (SQLException e) {
            getServletContext().log("Expense deletion error", e);
            response.setStatus(500);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Unable to delete expense\"}");
        }
    }
}
