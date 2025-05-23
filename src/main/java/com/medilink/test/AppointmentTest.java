package com.medilink.test;

import com.medilink.model.Appointment;
import com.medilink.model.Doctor;
import com.medilink.model.Patient;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentTest {
    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("medilinkPU");
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Doctor doctor = em.find(Doctor.class, 9L);
            Patient patient = em.find(Patient.class, 3L);

            if (doctor == null || patient == null) {
                System.out.println("Doctor or Patient not found. Please create them first.");
                return;
            }

            // Check for existing appointment with same doctor, date, and time
            boolean exists = !em.createQuery(
                "SELECT a FROM Appointment a WHERE a.doctor = :doctor AND a.date = :date AND a.time = :time"
            )
            .setParameter("doctor", doctor)
            .setParameter("date", LocalDate.now().plusDays(1))
            .setParameter("time", LocalTime.of(10, 30))
            .getResultList().isEmpty();

            if (exists) {
                System.out.println("Conflict: Doctor already has an appointment at this date and time.");
                return;
            }

            Appointment appointment = new Appointment();
            appointment.setDate(LocalDate.now().plusDays(1));
            appointment.setTime(LocalTime.of(10, 30));
            appointment.setDoctor(doctor);
            appointment.setPatient(patient);
            appointment.setDescription("Routine checkup and follow-up");

            em.persist(appointment);
            em.getTransaction().commit();
            System.out.println("Appointment created with id : " + appointment.getId());
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
            emf.close();
        }
    }
} 