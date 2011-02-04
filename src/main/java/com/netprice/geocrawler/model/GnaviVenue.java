package com.netprice.geocrawler.model;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value="gnavi")
@Cache(usage= CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class GnaviVenue extends Venue {}
