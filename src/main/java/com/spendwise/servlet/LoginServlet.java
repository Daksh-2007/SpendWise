package com.spendwise.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        String username = System.getenv("SPENDWISE_USERNAME");
        String password = System.getenv("SPENDWISE_PASSWORD");

        String enteredUsername = request.getParameter("username");
        String enteredPassword = request.getParameter("password");

        if (username != null && password != null
                && username.equals(enteredUsername)
                && password.equals(enteredPassword)) {

            HttpSession session = request.getSession(true);
            request.changeSessionId();
            session.setAttribute("loggedIn", true);

            response.sendRedirect(request.getContextPath() + "/index.html");

        } else {
            response.sendRedirect(
                    request.getContextPath() + "/login.html?error=1");
        }
    }
}