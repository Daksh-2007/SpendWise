package com.spendwise.servlet;

import com.spendwise.dao.CategoryDAO;
import com.spendwise.model.Category;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/api/categories")
public class CategoryServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            List<Category> categories = new CategoryDAO().getAllCategories();

            StringBuilder json = new StringBuilder("[");

            for (int i = 0; i < categories.size(); i++) {
                Category c = categories.get(i);

                if (i > 0)
                    json.append(",");

                json.append("{")
                        .append("\"id\":").append(c.getId()).append(",")
                        .append("\"name\":\"").append(c.getName()).append("\"")
                        .append("}");
            }

            json.append("]");
            response.getWriter().write(json.toString());

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Database error\"}");
            e.printStackTrace();
        }
    }
}