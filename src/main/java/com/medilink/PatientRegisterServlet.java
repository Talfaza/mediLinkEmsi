package com.medilink;

import com.medilink.model.Patient;
import com.medilink.model.Doctor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.time.LocalDate;
import java.util.Properties;
import java.util.UUID;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class PatientRegisterServlet extends HttpServlet {
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
            System.out.println("Received JSON: " + json.toString());
            String firstName = json.get("firstName").asText();
            String lastName = json.get("lastName").asText();
            String email = json.get("email").asText();
            String password = json.get("password").asText();
            LocalDate birthday = LocalDate.parse(json.get("birthday").asText());
            String cornicDisease = json.has("cornicDisease") && !json.get("cornicDisease").isNull() 
                ? json.get("cornicDisease").asText() 
                : null;
            System.out.println("Parsed fields: firstName=" + firstName + ", lastName=" + lastName + ", email=" + email + ", birthday=" + birthday + ", cornicDisease=" + cornicDisease);

            EntityManager em = emf.createEntityManager();
            
            // Check if email exists in either doctors or patients
            boolean emailExists = em.createQuery("SELECT COUNT(d) FROM Doctor d WHERE d.email = :email", Long.class)
                    .setParameter("email", email)
                    .getSingleResult() > 0;
            
            if (!emailExists) {
                emailExists = em.createQuery("SELECT COUNT(p) FROM Patient p WHERE p.email = :email", Long.class)
                        .setParameter("email", email)
                        .getSingleResult() > 0;
            }

            if (emailExists) {
                out.write("{\"success\":false,\"message\":\"Email already registered.\"}");
                return;
            }

            em.getTransaction().begin();
            Patient patient = new Patient();
            patient.setFirstName(firstName);
            patient.setLastName(lastName);
            patient.setEmail(email);
            patient.setPassword(password);
            patient.setBirthday(birthday);
            patient.setCornicDisease(cornicDisease);
            String token = UUID.randomUUID().toString();
            patient.setVerificationToken(token);
            patient.setVerified(false);
            em.persist(patient);
            em.getTransaction().commit();
            em.close();

            sendVerificationEmail(email, token);

            out.write("{\"success\":true,\"message\":\"Registration successful. Please check your email to verify your account.\"}");
        } catch (Exception e) {
            e.printStackTrace();
            out.write("{\"success\":false,\"message\":\"Registration failed.\"}");
        }
    }

    private void sendVerificationEmail(String toEmail, String token) {
        final String username = "noreply.medilink@gmail.com";
        final String password = "uhfgaccpcgsdbjaq";
        String subject = "Medilink Email Verification";
        String verificationLink = "http://localhost:8080/medilink/verify?token=" + token;
        String html = "<!DOCTYPE html><html><body style=\"font-family: Arial, sans-serif; background: #f7f7f7; padding: 30px;\">"
            + "<div style=\"max-width: 500px; margin: auto; background: #fff; border-radius: 8px; box-shadow: 0 2px 8px #e0e0e0; padding: 30px;\">"
            + "<h1 style=\"color: #1976d2; text-align: center; font-size: 2em;\">MediLink Email Verification</h1>"
            + "<p style=\"font-size: 1.1em; text-align: center;\">Please verify your email address by clicking the button below:</p>"
            + "<div style=\"text-align: center; margin: 30px 0;\">"
            + "<a href=\"" + verificationLink + "\" style=\"background: #1976d2; color: #fff; padding: 15px 30px; border-radius: 5px; text-decoration: none; font-size: 1.1em; font-weight: bold; display: inline-block;\">"
            + "Verify Account</a></div>"
            + "<p style=\"color: #888; font-size: 0.9em; text-align: center;\">If you did not request this, you can ignore this email.</p>"
            + "</div></body></html>";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(html, "text/html");
            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        if (emf != null) {
            emf.close();
        }
    }
} 