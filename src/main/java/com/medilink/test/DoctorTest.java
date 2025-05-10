package com.medilink.test;

import com.medilink.model.Doctor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.UUID;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class DoctorTest {
    public static void main(String[] args) {
        try {
            // Create EntityManagerFactory
            EntityManagerFactory emf = Persistence.createEntityManagerFactory("medilinkPU");
            EntityManager em = emf.createEntityManager();

            // Start transaction
            em.getTransaction().begin();

            // Create a new doctor
            Doctor doctor = new Doctor();
            doctor.setFirstName("John");
            doctor.setLastName("Doe");
            doctor.setEmail("charifyahia5@gmail.com");
            doctor.setPassword("password123");
            doctor.setSpecialty("Cardiology");

            // Generate verification token
            String token = UUID.randomUUID().toString();
            doctor.setVerificationToken(token);
            doctor.setVerified(false);

            // Save the doctor
            em.persist(doctor);
            System.out.println("Doctor saved with ID: " + doctor.getId());

            // Send verification email
            sendVerificationEmail(doctor.getEmail(), token);

            // Commit transaction
            em.getTransaction().commit();

            // Retrieve the doctor
            Doctor retrievedDoctor = em.find(Doctor.class, doctor.getId());
            System.out.println("Retrieved doctor: " + retrievedDoctor.getFirstName() + " " + retrievedDoctor.getLastName());

            // Close resources
            em.close();
            emf.close();

            System.out.println("Test completed successfully!");

        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void sendVerificationEmail(String toEmail, String token) {
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
            System.out.println("Verification email sent to: " + toEmail);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
} 