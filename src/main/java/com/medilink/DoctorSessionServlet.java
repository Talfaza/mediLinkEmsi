package com.medilink;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DoctorSessionServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        boolean loggedIn = req.getSession(false) != null && req.getSession(false).getAttribute("doctorEmail") != null;
        resp.getWriter().write("{\"loggedIn\":" + loggedIn + "}");
    }
} 