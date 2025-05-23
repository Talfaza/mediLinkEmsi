package com.medilink;

import com.medilink.model.Appointment;
import com.medilink.model.Doctor;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.annotation.JsonFormat;

public class DoctorAppointmentServlet extends HttpServlet {
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
        String doctorEmail = (String) req.getSession(false).getAttribute("doctorEmail");
        if (doctorEmail == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"error\":\"Not authenticated\"}");
            return;
        }
        EntityManager em = emf.createEntityManager();
        try {
            Doctor doctor = em.createQuery("SELECT d FROM Doctor d WHERE d.email = :email", Doctor.class)
                .setParameter("email", doctorEmail)
                .getSingleResult();
            
            List<Appointment> appointments = em.createQuery(
                "SELECT a FROM Appointment a WHERE a.doctor = :doctor ORDER BY a.date, a.time", Appointment.class)
                .setParameter("doctor", doctor)
                .getResultList();
            
            // Create DTOs to avoid lazy loading issues
            List<AppointmentDTO> appointmentDTOs = appointments.stream()
                .map(a -> new AppointmentDTO(
                    a.getId(),
                    a.getDate(),
                    a.getTime(),
                    a.getPatient().getId(),
                    a.getPatient().getFirstName() + " " + a.getPatient().getLastName(),
                    a.getDescription()
                ))
                .collect(Collectors.toList());
            
            out.write(mapper.writeValueAsString(appointmentDTOs));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String errorMsg = "{\"error\":\"Failed to fetch appointments\",\"details\":\"" + e.getClass().getSimpleName() + ": " + e.getMessage() + "\"}";
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
        String doctorEmail = (String) req.getSession(false).getAttribute("doctorEmail");
        if (doctorEmail == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"error\":\"Not authenticated\"}");
            return;
        }
        try {
            JsonNode json = mapper.readTree(req.getInputStream());
            long patientId = json.get("patientId").asLong();
            LocalDate date = LocalDate.parse(json.get("date").asText());
            LocalTime time = LocalTime.parse(json.get("time").asText());
            String description = json.has("description") ? json.get("description").asText() : "";

            EntityManager em = emf.createEntityManager();
            em.getTransaction().begin();
            Doctor doctor = em.createQuery("SELECT d FROM Doctor d WHERE d.email = :email", Doctor.class)
                .setParameter("email", doctorEmail)
                .getSingleResult();
            Patient patient = em.find(Patient.class, patientId);
            if (patient == null) {
                out.write("{\"success\":false,\"message\":\"Patient not found.\"}");
                em.getTransaction().rollback();
                em.close();
                return;
            }
            boolean exists = !em.createQuery(
                "SELECT a FROM Appointment a WHERE a.doctor = :doctor AND a.date = :date AND a.time = :time"
            )
            .setParameter("doctor", doctor)
            .setParameter("date", date)
            .setParameter("time", time)
            .getResultList().isEmpty();
            if (exists) {
                out.write("{\"success\":false,\"message\":\"Conflict: Doctor already has an appointment at this date and time.\"}");
                em.getTransaction().rollback();
                em.close();
                return;
            }
            Appointment appointment = new Appointment();
            appointment.setDate(date);
            appointment.setTime(time);
            appointment.setDoctor(doctor);
            appointment.setPatient(patient);
            appointment.setDescription(description);
            em.persist(appointment);
            em.getTransaction().commit();
            out.write("{\"success\":true,\"id\":" + appointment.getId() + "}");
            em.close();
        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"success\":false,\"message\":\"Failed to create appointment.\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        PrintWriter out = resp.getWriter();
        String doctorEmail = (String) req.getSession(false).getAttribute("doctorEmail");
        if (doctorEmail == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"error\":\"Not authenticated\"}");
            return;
        }
        String idParam = req.getParameter("id");
        if (idParam == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\":\"Missing appointment id\"}");
            return;
        }
        Long apptId = Long.parseLong(idParam);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Appointment appt = em.find(Appointment.class, apptId);
            if (appt == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"error\":\"Appointment not found\"}");
                em.getTransaction().rollback();
                return;
            }
            Doctor doctor = em.createQuery("SELECT d FROM Doctor d WHERE d.email = :email", Doctor.class)
                .setParameter("email", doctorEmail)
                .getSingleResult();
            if (!appt.getDoctor().getId().equals(doctor.getId())) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write("{\"error\":\"Not authorized\"}");
                em.getTransaction().rollback();
                return;
            }
            em.remove(appt);
            em.getTransaction().commit();
            out.write("{\"success\":true}");
        } catch (Exception e) {
            em.getTransaction().rollback();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\":\"Failed to delete appointment\"}");
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        PrintWriter out = resp.getWriter();
        String doctorEmail = (String) req.getSession(false).getAttribute("doctorEmail");
        if (doctorEmail == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"error\":\"Not authenticated\"}");
            return;
        }
        try {
            JsonNode json = mapper.readTree(req.getInputStream());
            Long apptId = json.get("id").asLong();
            LocalDate newDate = LocalDate.parse(json.get("date").asText());
            LocalTime newTime = LocalTime.parse(json.get("time").asText());
            EntityManager em = emf.createEntityManager();
            em.getTransaction().begin();
            Appointment appt = em.find(Appointment.class, apptId);
            if (appt == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"error\":\"Appointment not found\"}");
                em.getTransaction().rollback();
                em.close();
                return;
            }
            Doctor doctor = em.createQuery("SELECT d FROM Doctor d WHERE d.email = :email", Doctor.class)
                .setParameter("email", doctorEmail)
                .getSingleResult();
            if (!appt.getDoctor().getId().equals(doctor.getId())) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.write("{\"error\":\"Not authorized\"}");
                em.getTransaction().rollback();
                em.close();
                return;
            }
            // Check for conflict
            boolean exists = !em.createQuery(
                "SELECT a FROM Appointment a WHERE a.doctor = :doctor AND a.date = :date AND a.time = :time AND a.id <> :id"
            )
            .setParameter("doctor", doctor)
            .setParameter("date", newDate)
            .setParameter("time", newTime)
            .setParameter("id", apptId)
            .getResultList().isEmpty();
            if (exists) {
                out.write("{\"success\":false,\"message\":\"Conflict: Doctor already has an appointment at this date and time.\"}");
                em.getTransaction().rollback();
                em.close();
                return;
            }
            appt.setDate(newDate);
            appt.setTime(newTime);
            em.merge(appt);
            em.getTransaction().commit();
            out.write("{\"success\":true}");
            em.close();
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\":\"Failed to reschedule appointment\"}");
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        if (emf != null) {
            emf.close();
        }
    }

    // DTO to avoid lazy loading issues
    private static class AppointmentDTO {
        private Long id;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate date;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
        private LocalTime time;
        private Long patientId;
        private String patientName;
        private String description;

        public AppointmentDTO(Long id, LocalDate date, LocalTime time, Long patientId, String patientName, String description) {
            this.id = id;
            this.date = date;
            this.time = time;
            this.patientId = patientId;
            this.patientName = patientName;
            this.description = description;
        }

        // Getters
        public Long getId() { return id; }
        public LocalDate getDate() { return date; }
        public LocalTime getTime() { return time; }
        public Long getPatientId() { return patientId; }
        public String getPatientName() { return patientName; }
        public String getDescription() { return description; }
    }
} 