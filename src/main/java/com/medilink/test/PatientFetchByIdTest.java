package com.medilink.test;

import com.medilink.model.Patient;
import com.medilink.model.Appointment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.List;

public class PatientFetchByIdTest {
    public static void main(String[] args) {
        try {
            EntityManagerFactory emf = Persistence.createEntityManagerFactory("medilinkPU");
            EntityManager em = emf.createEntityManager();

            Patient patient14 = em.find(Patient.class, 12L);
            if (patient14 != null) {
                System.out.println("Patient with ID 12: " + patient14.getFirstName() + " " + patient14.getLastName() + ", Email: " + patient14.getEmail());
                List<Appointment> appointments = em.createQuery(
                    "SELECT a FROM Appointment a WHERE a.patient = :patient ORDER BY a.date, a.time", Appointment.class)
                    .setParameter("patient", patient14)
                    .getResultList();
                if (appointments.isEmpty()) {
                    System.out.println("No appointments found for patient with ID 14");
                } else {
                    System.out.println("Appointments for patient with ID 14:");
                    for (Appointment a : appointments) {
                        String doctorName = "Dr. " + a.getDoctor().getFirstName() + " " + a.getDoctor().getLastName();
                        System.out.println("- Date: " + a.getDate() + ", Time: " + a.getTime() + ", Doctor: " + doctorName + ", Description: " + a.getDescription());
                    }
                }
            } else {
                System.out.println("No patient found with ID 14");
            }

            em.close();
            emf.close();
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 