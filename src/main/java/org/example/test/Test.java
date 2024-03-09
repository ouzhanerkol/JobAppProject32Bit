package org.example.test;

import org.example.entities.Forex;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.util.Date;

public class Test {
    public static void main(String[] args) {

        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("persistence-unit");
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction entityTransaction = entityManager.getTransaction();

        Forex forex = new Forex(1, new Date(), "USD", 1, 11.24, 11);

        entityTransaction.begin();

        entityManager.persist(forex);

        entityTransaction.commit();
    }
}
