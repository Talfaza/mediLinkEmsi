package com.medilink;

import com.medilink.model.Patient;
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

public class PatientLoginServlet extends HttpServlet {
    private EntityManagerFactory emf;

    @Override
    public void init() throws ServletException {
        try {
            emf = Persistence.createEntityManagerFactory("medilinkPU");
            System.out.println("EntityManagerFactory created successfully");
        } catch (Exception e) {
            System.err.println("Error creating EntityManagerFactory: " + e.getMessage());
            e.printStackTrace();
            throw new ServletException("Failed to initialize EntityManagerFactory", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        ObjectMapper mapper = new ObjectMapper();
        PrintWriter out = resp.getWriter();
        try {
            JsonNode json = mapper.readTree(req.getInputStream());
            System.out.println("Login received JSON: " + json.toString());
            String email = json.get("email").asText();
            String password = json.get("password").asText();
            System.out.println("Parsed login fields: email=" + email + ", password=" + password);

            EntityManager em = null;
            try {
                em = emf.createEntityManager();
                System.out.println("EntityManager created successfully");
                
                Patient patient = em.createQuery("SELECT p FROM Patient p WHERE p.email = :email", Patient.class)
                        .setParameter("email", email)
                        .getResultStream().findFirst().orElse(null);
                
                if (patient == null) {
                    System.out.println("No patient found with email: " + email);
                } else {
                    System.out.println("Found patient: " + patient.getFirstName() + " " + patient.getLastName());
                }

                if (patient != null && patient.getPassword().equals(password) && patient.isVerified()) {
                    // Set session attribute
                    req.getSession().setAttribute("patientEmail", email);
                    resp.setStatus(HttpServletResponse.SC_OK);
                    out.write("{\"success\":true,\"message\":\"Login successful.\"}");
                    System.out.println("Login success for: " + email);
                } else if (patient != null && !patient.isVerified()) {
                    out.write("{\"success\":false,\"message\":\"Please verify your email before logging in.\"}");
                    System.out.println("Login failed: email not verified for " + email);
                } else if (patient != null) {
                    out.write("{\"success\":false,\"message\":\"Invalid email or password.\"}");
                    System.out.println("Login failed: wrong password for " + email);
                } else {
                    out.write("{\"success\":false,\"message\":\"Invalid email or password.\"}");
                    System.out.println("Login failed: email not found " + email);
                }
            } finally {
                if (em != null) {
                    em.close();
                    System.out.println("EntityManager closed");
                }
            }
        } catch (Exception e) {
            System.err.println("Error during login: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"success\":false,\"message\":\"Login failed: " + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    @Override
    public void destroy() {
        if (emf != null) {
            emf.close();
            System.out.println("EntityManagerFactory closed");
        }
    }
} 