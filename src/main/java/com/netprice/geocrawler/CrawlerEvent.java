package com.netprice.geocrawler;

import com.netprice.geocrawler.model.Venue;
import java.util.List;

public class CrawlerEvent {

    private List<Venue> venues;
    private EventType type; 

    /**
     * @return the venues
     */
    public List<Venue> getVenues() {
        return venues;
    }

    /**
     * @param venues the venues to set
     */
    public void setVenues(List<Venue> venues) {
        this.venues = venues;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }
}
