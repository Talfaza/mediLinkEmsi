package com.medilink;

import com.medilink.model.Appointment;
import com.medilink.model.Patient;
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
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class PatientAppointmentServlet extends HttpServlet {
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

        // Check if session exists
        if (req.getSession(false) == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"error\":\"No active session\"}");
            return;
        }

        String patientEmail = (String) req.getSession(false).getAttribute("patientEmail");
        if (patientEmail == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"error\":\"Not authenticated\"}");
            return;
        }

        EntityManager em = emf.createEntityManager();
        try {
            // Fetch patient with a single query
            Patient patient = em.createQuery(
                "SELECT p FROM Patient p WHERE p.email = :email", Patient.class)
                .setParameter("email", patientEmail)
                .getSingleResult();

            // Fetch appointments with doctor information in a single query
            List<Appointment> appointments = em.createQuery(
                "SELECT a FROM Appointment a " +
                "LEFT JOIN FETCH a.doctor " +
                "WHERE a.patient = :patient " +
                "ORDER BY a.date, a.time", Appointment.class)
                .setParameter("patient", patient)
                .getResultList();

            List<AppointmentDTO> appointmentDTOs = appointments.stream()
                .map(a -> new AppointmentDTO(
                    a.getId(),
                    a.getDate().toString(),
                    a.getTime().toString(),
                    a.getDescription(),
                    "Dr. " + a.getDoctor().getFirstName() + " " + a.getDoctor().getLastName()
                ))
                .collect(Collectors.toList());

            out.write(mapper.writeValueAsString(appointmentDTOs));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String errorMsg = String.format("{\"error\":\"Failed to fetch appointments\",\"details\":\"%s: %s\"}", 
                e.getClass().getSimpleName(), e.getMessage());
            out.write(errorMsg);
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        PrintWriter out = resp.getWriter();

        // Check if session exists
        if (req.getSession(false) == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"error\":\"No active session\"}");
            return;
        }
        String patientEmail = (String) req.getSession(false).getAttribute("patientEmail");
        if (patientEmail == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"error\":\"Not authenticated\"}");
            return;
        }

        // Parse request body
        String requestBody = req.getReader().lines().collect(Collectors.joining());
        CreateAppointmentRequest appointmentRequest;
        try {
            appointmentRequest = mapper.readValue(requestBody, CreateAppointmentRequest.class);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\":\"Invalid request format\"}");
            return;
        }
        if (appointmentRequest.doctorId == null || appointmentRequest.date == null || appointmentRequest.time == null || appointmentRequest.description == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\":\"Invalid appointment data\"}");
            return;
        }
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            // Get patient
            Patient patient = em.createQuery(
                "SELECT p FROM Patient p WHERE p.email = :email", Patient.class)
                .setParameter("email", patientEmail)
                .getSingleResult();
            // Get doctor
            Doctor doctor = em.find(Doctor.class, appointmentRequest.doctorId);
            if (doctor == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"error\":\"Doctor not found\"}");
                return;
            }
            // Check for time conflicts
            java.time.LocalDate date = java.time.LocalDate.parse(appointmentRequest.date);
            java.time.LocalTime time = java.time.LocalTime.parse(appointmentRequest.time);
            List<Appointment> conflicts = em.createQuery(
                "SELECT a FROM Appointment a WHERE a.doctor = :doctor AND a.date = :date AND a.time = :time", Appointment.class)
                .setParameter("doctor", doctor)
                .setParameter("date", date)
                .setParameter("time", time)
                .getResultList();
            if (!conflicts.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                out.write("{\"error\":\"Time slot is already booked\"}");
                return;
            }
            // Create appointment
            Appointment appointment = new Appointment();
            appointment.setPatient(patient);
            appointment.setDoctor(doctor);
            appointment.setDate(date);
            appointment.setTime(time);
            appointment.setDescription(appointmentRequest.description);
            em.persist(appointment);
            em.getTransaction().commit();
            resp.setStatus(HttpServletResponse.SC_CREATED);
            out.write("{\"message\":\"Appointment created successfully\",\"id\":" + appointment.getId() + "}");
        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\":\"Failed to create appointment\"}");
            e.printStackTrace();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    @Override
    public void destroy() {
        if (emf != null) {
            emf.close();
        }
    }

    private static class AppointmentDTO {
        public Long id;
        public String date;
        public String time;
        public String description;
        public String doctorName;

        public AppointmentDTO(Long id, String date, String time, String description, String doctorName) {
            this.id = id;
            this.date = date;
            this.time = time;
            this.description = description;
            this.doctorName = doctorName;
        }
    }

    private static class CreateAppointmentRequest {
        public Long doctorId;
        public String date;
        public String time;
        public String description;
    }
} 