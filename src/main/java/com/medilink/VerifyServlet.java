package com.medilink;

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

// @WebServlet("/verify")
public class VerifyServlet extends HttpServlet {
    private EntityManagerFactory emf;

    @Override
    public void init() throws ServletException {
        emf = Persistence.createEntityManagerFactory("medilinkPU");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        if (token == null || token.isEmpty()) {
            out.println("<h2>Invalid verification link.</h2>");
            return;
        }
        EntityManager em = emf.createEntityManager();
        boolean found = false;
        try {
            em.getTransaction().begin();
            // Try to find Doctor by token
            Doctor doctor = em.createQuery("SELECT d FROM Doctor d WHERE d.verificationToken = :token", Doctor.class)
                    .setParameter("token", token)
                    .getResultStream().findFirst().orElse(null);
            if (doctor != null && !doctor.isVerified()) {
                doctor.setVerified(true);
                doctor.setVerificationToken(null);
                em.merge(doctor);
                found = true;
                out.println("<h2>Doctor email verified successfully! You can now log in.</h2>");
            }
            // Try to find Patient by token if not found as Doctor
            if (!found) {
                Patient patient = em.createQuery("SELECT p FROM Patient p WHERE p.verificationToken = :token", Patient.class)
                        .setParameter("token", token)
                        .getResultStream().findFirst().orElse(null);
                if (patient != null && !patient.isVerified()) {
                    patient.setVerified(true);
                    patient.setVerificationToken(null);
                    em.merge(patient);
                    found = true;
                    out.println("<h2>Patient email verified successfully! You can now log in.</h2>");
                }
            }
            em.getTransaction().commit();
            if (!found) {
                out.println("<h2>Invalid or already used verification token.</h2>");
            }
        } catch (Exception e) {
            em.getTransaction().rollback();
            out.println("<h2>Error during verification. Please try again later.</h2>");
            e.printStackTrace(out);
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
} 