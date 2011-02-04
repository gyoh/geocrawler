package com.netprice.geocrawler;

import com.netprice.geocrawler.listener.CrawlerListener;
import com.netprice.geocrawler.manager.PropertyManager;
import com.netprice.geocrawler.model.GnaviVenue;
import com.netprice.geocrawler.model.Venue;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author gyo
 */
public class GnaviCrawler extends Crawler {

    private static final Logger logger =
            LoggerFactory.getLogger(GnaviCrawler.class);

    private static final String SCHEME = "http";
    private static final String HOST = "api.gnavi.co.jp";
    private static final int PORT = 80;
    private static final String PATH_REST = "/ver1/RestSearchAPI/";
    private static final String PATH_PREF = "/ver1/PrefSearchAPI/";
    private static final String PATH_CTG = "/ver1/CategorySmallSearchAPI/";
    private static final String KEY_ID = "5c30463d884796d3c7e882d5537f70ed";
    private static final String COORDINATES_MODE = "2"; // WGS
    private static final String SORT = "1";
    private static final String OFFSET = "1";
    private static final int HIT_PER_PAGE = 100;

    protected void fetch() {
        // Create an instance of HttpClient.
        HttpClient client = new HttpClient();
        client.getHostConfiguration().setHost(HOST, PORT, SCHEME);

        List<String> prefectures = getCodes(client, PATH_PREF, "pref");
        List<String> categories = getCodes(client, PATH_CTG, "category_s");

        // This is a workaround for the gnavi API.
        // Gnavi API only allows max 1000 records for output.
        // Therefore, we divide records into smaller pieces
        // by prefectures and categories.
        for (String prefecture: prefectures) {
            for (String category: categories) {
                logger.debug("Prefecture: {}", prefecture);
                logger.debug("Category: {}", category);
                getVenues(client, prefecture, category, 1);
            }
        }
    }

    private List<String> getCodes(HttpClient client, String path, String tag) {
        // Create a method instance.
        GetMethod getMethod = new GetMethod(path);
        getMethod.addRequestHeader("User-Agent",
                PropertyManager.getInstance().getUserAgent());
        getMethod.setQueryString(new NameValuePair[] {
            new NameValuePair("keyid", KEY_ID)
        });

        List<String> codes = new ArrayList<String>();

        try {
            // Execute the method.
            int statusCode = client.executeMethod(getMethod);
            if (statusCode != HttpStatus.SC_OK) {
                logger.error("Method failed: {}", getMethod.getStatusLine());
                return codes;
            }

            // Read the response body.
            InputStream responseBodyAsStream =
                    getMethod.getResponseBodyAsStream();
            BufferedInputStream bis =
                new BufferedInputStream(responseBodyAsStream);

            // Deal with the response.
            Source src = new Source(bis);
            src.fullSequentialParse();

            List<Element> elements = src.getAllElements(tag);
            for (Element element: elements) {
                codes.add(element.getFirstElement(tag + "_code")
                        .getContent().toString());
            }
        } catch (HttpException e) {
            logger.error("Fatal protocol violation", e);
        } catch (IOException e) {
            logger.error("Fatal transport error", e);
        } catch (IllegalArgumentException e) {
            logger.error("Illegal or inappropriate argument", e);
        } catch (Exception e) {
            logger.error("Exception occurred", e);
        } finally {
            // Release the connection.
            getMethod.releaseConnection();
        }

        return codes;
    }

    private void getVenues(HttpClient client,
            String prefecture, String category, int pageId) {
        logger.info("Page " + pageId);

        // Wait for a second to prevent DoS.
        CrawlerUtils.pause(1);

        // Create a method instance.
        GetMethod getMethod = new GetMethod(PATH_REST);
        getMethod.addRequestHeader("User-Agent",
                PropertyManager.getInstance().getUserAgent());
        getMethod.setQueryString(new NameValuePair[] {
            new NameValuePair("keyid", KEY_ID),
            new NameValuePair("pref", prefecture),
            new NameValuePair("category_s", category),
            new NameValuePair("coordinates_mode", COORDINATES_MODE),
            new NameValuePair("sort", SORT),
            new NameValuePair("offset", OFFSET),
            new NameValuePair("hit_per_page", String.valueOf(HIT_PER_PAGE)),
            new NameValuePair("offset_page", String.valueOf(pageId))
        });

        // CrawlerEvent holds the list of venues to pass to the listeners.
        List<Venue> venues = new ArrayList<Venue>();
        CrawlerEvent event = new CrawlerEvent();
        event.setVenues(venues);
        event.setType(EventType.GNAVI);

        int total = 0; // Total number of venues
        try {
            // Execute the method.
            int statusCode = client.executeMethod(getMethod);
            if (statusCode != HttpStatus.SC_OK) {
                logger.error("Method failed: {}", getMethod.getStatusLine());
                return;
            }

            // Read the response body.
            InputStream responseBodyAsStream =
                    getMethod.getResponseBodyAsStream();
            BufferedInputStream bis =
                new BufferedInputStream(responseBodyAsStream);

            // Deal with the response.
            Source src = new Source(bis);
            src.fullSequentialParse();

            // Terminate if we get an error code.
            Element error = src.getFirstElement("error");
            if (error != null) {
                String errorStr = error.getFirstElement("code")
                        .getContent().toString();
                int errorCode;
                try {
                    errorCode = Integer.parseInt(errorStr);
                } catch (NumberFormatException e) {
                    logger.error("Failed to parse error code: {}", errorStr);
                    return;
                }

                switch (errorCode) {
                    case 600:
                        logger.info("No venues found.");
                        return;
                    case 601:
                        logger.error("Invalid access.");
                        return;
                    case 602:
                        logger.error("Invalid shop number.");
                        return;
                    case 603:
                        logger.error("Invalid type.");
                        return;
                    case 604:
                        logger.error("Internal server error.");
                        return;
                    default:
                        logger.error("Unknown error occurred: {}", errorCode);
                        return;
                }
            }

            // Get the total count.
            String totalStr = src.getFirstElement("total_hit_count")
                    .getContent().toString();
            total = Integer.parseInt(totalStr);
            logger.info("total venues: {}", total);

            // Generate a list of venues.
            List<Element> elements = src.getAllElements("rest");
            for (Element element: elements) {
                Venue venue = new GnaviVenue();
                venue.setName(element.getFirstElement("name")
                        .getContent().toString());
                String[] address = splitAddress(
                        element.getFirstElement("address")
                        .getContent().toString());
                venue.setAddress(address[1]);
                venue.setZip(address[0]);
                String latStr = element.getFirstElement("latitude")
                        .getContent().toString();
                double latitude = Double.parseDouble(latStr);
                venue.setLatitude(latitude);
                String longStr = element.getFirstElement("longitude")
                        .getContent().toString();
                double longitude = Double.parseDouble(longStr);
                venue.setLongitude(longitude);
                venue.setUrl(element.getFirstElement("url")
                        .getContent().toString().split("\\?")[0]);
                String updateStr = element.getFirstElement("update_date")
                        .getContent().toString();
                Date lastUpdated = parseDate(updateStr);
                venue.setLastUpdated(lastUpdated);
                venues.add(venue);
            }
        } catch (HttpException e) {
            logger.error("Fatal protocol violation", e);
        } catch (IOException e) {
            logger.error("Fatal transport error", e);
        } catch (IllegalArgumentException e) {
            logger.error("Illegal or inappropriate argument", e);
        } catch (Exception e) {
            logger.error("Exception occurred", e);
        } finally {
            // Release the connection.
            getMethod.releaseConnection();
        }

        // Propagate event to listeners
        for (CrawlerListener listener : listeners) {
            listener.crawlerEvent(event);
        }

        // Go to next page
        if (HIT_PER_PAGE * pageId  < total) {
            getVenues(client, prefecture, category, ++pageId);
        }
    }

    private Date parseDate(String dateStr) {
        Date date = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            date = sdf.parse(dateStr);
        } catch (ParseException e) {
            logger.error("Invalid date", e);
        }
        return date;
    }

    private String[] splitAddress(String address) {
        String[] result = address.split(" ");
        return new String[] {result[0].substring(1), result[1]};
    }

}
