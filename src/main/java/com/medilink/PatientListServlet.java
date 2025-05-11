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
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class PatientListServlet extends HttpServlet {
    private EntityManagerFactory emf;

    @Override
    public void init() throws ServletException {
        emf = Persistence.createEntityManagerFactory("medilinkPU");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        PrintWriter out = resp.getWriter();
        
        // Check if doctor is authenticated
        String doctorEmail = (String) req.getSession(false).getAttribute("doctorEmail");
        if (doctorEmail == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"error\":\"Not authenticated\"}");
            return;
        }

        EntityManager em = emf.createEntityManager();
        try {
            List<Patient> patients = em.createQuery("SELECT p FROM Patient p WHERE p.verified = true", Patient.class)
                .getResultList();
            
            // Create a simplified version of patients without sensitive data
            List<PatientDTO> patientDTOs = patients.stream()
                .map(p -> new PatientDTO(p.getId(), p.getFirstName(), p.getLastName(), p.getEmail()))
                .collect(Collectors.toList());
            
            out.write(mapper.writeValueAsString(patientDTOs));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\":\"Failed to fetch patients\"}");
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    @Override
    public void destroy() {
        if (emf != null) {
            emf.close();
        }
    }

    // DTO to return only necessary patient information
    private static class PatientDTO {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;

        public PatientDTO(Long id, String firstName, String lastName, String email) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }

        // Getters
        public Long getId() { return id; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getEmail() { return email; }
    }
} 