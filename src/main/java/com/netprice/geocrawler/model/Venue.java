package com.netprice.geocrawler.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name="venue")
@DiscriminatorColumn(name="type")
@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Venue implements Serializable {
    private long id;
    private String name;
    private String address;
    private String zip;
    private double latitude;
    private double longitude;
    private long woeid;
    private String url;
    private Date dateCreated;
    private Date lastUpdated;

    @Id
    @GeneratedValue
    @Column(unique = true, nullable = false)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(nullable = false)
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    @Column(nullable = false)
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    @Column(nullable = false)
    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getWoeid() {
        return woeid;
    }

    public void setWoeid(long woeid) {
        this.woeid = woeid;
    }

    @Column(nullable = false)
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @PrePersist
    protected void onCreate() {
        this.dateCreated = new Date();
    }
}
