package com.medilink;

import com.medilink.model.Doctor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class DoctorLoginServlet extends HttpServlet {
    private EntityManagerFactory emf;

    @Override
    public void init() throws ServletException {
        emf = Persistence.createEntityManagerFactory("medilinkPU");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        ObjectMapper mapper = new ObjectMapper();
        PrintWriter out = resp.getWriter();
        try {
            JsonNode json = mapper.readTree(req.getInputStream());
            String email = json.get("email").asText();
            String password = json.get("password").asText();

            EntityManager em = emf.createEntityManager();
            Doctor doctor = em.createQuery("SELECT d FROM Doctor d WHERE d.email = :email", Doctor.class)
                    .setParameter("email", email)
                    .getResultStream().findFirst().orElse(null);
            em.close();

            if (doctor != null && doctor.getPassword().equals(password) && doctor.isVerified()) {
                // Set session attribute
                req.getSession().setAttribute("doctorEmail", email);
                // Redirect to frontend dashboard
                resp.setStatus(HttpServletResponse.SC_OK);
                out.write("{\"success\":true,\"redirect\":\"http://localhost:5173/doctor-dashboard\"}");
            } else if (doctor != null && !doctor.isVerified()) {
                out.write("{\"success\":false,\"message\":\"Please verify your email before logging in.\"}");
            } else {
                out.write("{\"success\":false,\"message\":\"Invalid email or password.\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"success\":false,\"message\":\"Login failed.\"}");
        }
    }

    @Override
    public void destroy() {
        if (emf != null) {
            emf.close();
        }
    }
} 