package com.netprice.geocrawler.listener;

import com.netprice.geocrawler.CrawlerEvent;
import com.netprice.geocrawler.model.Venue;
import java.util.List;

/**
 *
 * @author gyo
 */
public class PrintListener implements CrawlerListener {

    @Override
    public void crawlerEvent(CrawlerEvent event) {
        List<Venue> venues = event.getVenues();
        print(venues);
    }

    private void print(List<Venue> venues) {
        for (Venue venue: venues) {
            System.out.println("Name: " + venue.getName());
            System.out.println("Address: " + venue.getAddress());
            System.out.println("Zip: " + venue.getZip());
            System.out.println("Latitude: " + venue.getLatitude());
            System.out.println("Longitude: " + venue.getLongitude());
            System.out.println("WOEID: " + venue.getWoeid());
            System.out.println("Last updated: " + venue.getLastUpdated());
        }
    }

}
