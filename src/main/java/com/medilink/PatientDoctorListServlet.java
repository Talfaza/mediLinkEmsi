package com.medilink;

import com.medilink.model.Doctor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.logging.Logger;
import java.util.logging.Level;

public class PatientDoctorListServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(PatientDoctorListServlet.class.getName());
    private EntityManagerFactory emf;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        try {
            emf = Persistence.createEntityManagerFactory("medilinkPU");
            objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize PatientDoctorListServlet", e);
            throw new ServletException("Failed to initialize servlet", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Check if patient is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("patientEmail") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Not authenticated\"}");
            return;
        }

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            // Get all verified doctors
            List<Doctor> doctors = em.createQuery(
                "SELECT d FROM Doctor d WHERE d.verified = true", Doctor.class)
                .getResultList();

            // Convert to JSON and send response
            String jsonResponse = objectMapper.writeValueAsString(doctors);
            response.getWriter().write(jsonResponse);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching doctors list", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Failed to fetch doctors list\"}");
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    @Override
    public void destroy() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
} 