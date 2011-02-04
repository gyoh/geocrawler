package com.netprice.geocrawler.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Date;

public class PersistenceManager {

    private static final Logger logger =
            LoggerFactory.getLogger(PersistenceManager.class);

    private static final PersistenceManager INSTANCE =
            new PersistenceManager();

    private EntityManagerFactory emf;

    private PersistenceManager() {}

    public static PersistenceManager getInstance() {
        return INSTANCE;
    }

    public synchronized EntityManagerFactory getEntityManagerFactory() {
        if (emf == null) createEntityManagerFactory();
        return emf;
    }

    public EntityManager getEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    public void closeEntityManagerFactory() {
        if (emf != null) {
            emf.close();
            emf = null;
            logger.info("Persistence finished at {}", new Date());
        }
    }

    private void createEntityManagerFactory() {
        emf = Persistence.createEntityManagerFactory("geocrawler");
        logger.info("Persistence started at {}", new Date());

        // Make sure EntityManagerFactory is closed on JVM shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                closeEntityManagerFactory();
            }
        });
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        closeEntityManagerFactory();
    }

}