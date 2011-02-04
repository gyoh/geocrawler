package com.netprice.geocrawler.listener;

import com.netprice.geocrawler.CrawlerEvent;
import com.netprice.geocrawler.EventType;
import com.netprice.geocrawler.manager.PersistenceManager;
import com.netprice.geocrawler.model.GnaviVenue;
import com.netprice.geocrawler.model.HotpepperVenue;
import com.netprice.geocrawler.model.TabelogVenue;
import com.netprice.geocrawler.model.Venue;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gyo
 */
public class PersistenceListener implements CrawlerListener {

    private static final Logger logger =
            LoggerFactory.getLogger(PersistenceListener.class);

    @Override
    public void crawlerEvent(CrawlerEvent event) {
        List<Venue> venues = event.getVenues();
        try {
            persist(venues, event.getType());
        } catch(Exception e) {
            logger.error("Exception occurred", e);
        }
    }

    private void persist(List<Venue> newVenues, EventType type) {
        PersistenceManager pm = PersistenceManager.getInstance();
        EntityManager em = pm.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();

        String jpql;
        switch (type) {
            case GNAVI:
                jpql = "from GnaviVenue where url = :url";
                break;
            case TABELOG:
                jpql = "from TabelogVenue where url = :url";
                break;
            case HOTPEPPER:
                jpql = "from HotpepperVenue where url = :url";
                break;
            default:
                return;
        }

        // Define queries
        Query query = em.createQuery(jpql)
                .setHint("org.hibernate.cacheable", true);

        for (Venue newVenue: newVenues) {
            logger.info("Storing venue: {}", newVenue.getName());
            Venue venue;
            boolean isNewVenue = false;
            try {
                switch (type) {
                    case GNAVI:
                        venue = (GnaviVenue) query.setParameter(
                                "url", newVenue.getUrl()).getSingleResult();
                        break;
                    case TABELOG:
                        venue = (TabelogVenue) query.setParameter(
                                "url", newVenue.getUrl()).getSingleResult();
                        break;
                    case HOTPEPPER:
                        venue = (HotpepperVenue) query.setParameter(
                                "url", newVenue.getUrl()).getSingleResult();
                        break;
                    default:
                        continue;
                }
                logger.debug("Existing venue");
                logger.debug("ID: {}", venue.getId());
            } catch (NoResultException e) {
                switch (type) {
                    case GNAVI:
                        venue = new GnaviVenue();
                        break;
                    case TABELOG:
                        venue = new TabelogVenue();
                        break;
                    case HOTPEPPER:
                        venue = new HotpepperVenue();
                        break;
                    default:
                        continue;
                }
                logger.debug("New venue");
                isNewVenue = true;
            }

            venue.setName(newVenue.getName());
            logger.debug("Name: {}", venue.getName());
            venue.setAddress(newVenue.getAddress());
            logger.debug("Address: {}", venue.getAddress());
            venue.setZip(newVenue.getZip());
            logger.debug("Zip: {}", venue.getZip());
            venue.setLatitude(newVenue.getLatitude());
            logger.debug("Latitude: {}", venue.getLatitude());
            venue.setLongitude(newVenue.getLongitude());
            logger.debug("Longitude: {}", venue.getLongitude());
            venue.setWoeid(newVenue.getWoeid());
            logger.debug("WOEID: {}", venue.getWoeid());
            venue.setUrl(newVenue.getUrl());
            logger.debug("url: {}", venue.getUrl());
            venue.setLastUpdated(newVenue.getLastUpdated());
            logger.debug("Last updated: {}", venue.getLastUpdated());

            venue = em.merge(venue);
            if (isNewVenue) {
                logger.debug("ID: {}", venue.getId());
            }
        }

        tx.commit();
        em.close();
    }

}
