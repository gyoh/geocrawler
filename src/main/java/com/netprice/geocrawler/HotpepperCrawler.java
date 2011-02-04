package com.netprice.geocrawler;

import com.netprice.geocrawler.listener.CrawlerListener;
import com.netprice.geocrawler.manager.PropertyManager;
import com.netprice.geocrawler.model.HotpepperVenue;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 *
 * @author gyo
 */
public class HotpepperCrawler extends Crawler {

    private static final Logger logger =
            LoggerFactory.getLogger(HotpepperCrawler.class);

    private static final String SCHEME = "http";
    private static final String HOST = "webservice.recruit.co.jp";
    private static final int PORT = 80;
    private static final String PATH_GOURMET = "/hotpepper/gourmet/v1/";
    private static final String PATH_AREA = "/hotpepper/large_area/v1/";
    private static final String KEY = "16be4f78882293a9";
    private static final String DATUM = "world"; // WGS
    private static final String TYPE = "lite";
    private static final int COUNT = 100;

    protected void fetch() {
        // Create an instance of HttpClient.
        HttpClient client = new HttpClient();
        client.getHostConfiguration().setHost(HOST, PORT, SCHEME);

        List<String> prefectures = getPrefectures(client);

        for (String prefecture: prefectures) {
            logger.debug("Prefecture: {}", prefecture);
            getVenues(client, prefecture, 1);
        }
    }

    private List<String> getPrefectures(HttpClient client) {
        // Create a method instance.
        GetMethod getMethod = new GetMethod(PATH_AREA);
        getMethod.addRequestHeader("User-Agent",
                PropertyManager.getInstance().getUserAgent());
        getMethod.setQueryString(new NameValuePair[] {
            new NameValuePair("key", KEY)
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

            List<Element> elements = src.getAllElements("large_area");
            for (Element element: elements) {
                codes.add(element.getFirstElement("code")
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

    private void getVenues(
            HttpClient client, String prefecture, int start) {
        logger.info("start: {}", start);

        // Wait for a second to prevent DoS.
        CrawlerUtils.pause(1);

        // Create a method instance.
        GetMethod getMethod = new GetMethod(PATH_GOURMET);
        getMethod.addRequestHeader("User-Agent",
                PropertyManager.getInstance().getUserAgent());
        getMethod.setQueryString(new NameValuePair[] {
            new NameValuePair("key", KEY),
            new NameValuePair("large_area", prefecture),
            new NameValuePair("datum", DATUM),
            new NameValuePair("type", TYPE),
            new NameValuePair("count", String.valueOf(COUNT)),
            new NameValuePair("start", String.valueOf(start))
        });

        // CrawlerEvent holds the list of venues to pass to the listeners.
        List<Venue> venues = new ArrayList<Venue>();
        CrawlerEvent event = new CrawlerEvent();
        event.setVenues(venues);
        event.setType(EventType.HOTPEPPER);

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
                String message = error.getFirstElement("message")
                        .getContent().toString();
                logger.error(message);
                return;
            }

            // Get the total count.
            String totalStr = src.getFirstElement("results_available")
                    .getContent().toString();
            total = Integer.parseInt(totalStr);
            logger.info("total venues: {}", total);

            // Get the number of returned results.
            String countStr = src.getFirstElement("results_returned")
                    .getContent().toString();
            int count = Integer.parseInt(countStr);
            logger.info("results returned: {}", count);

            // Terminate if we have no more results returned.
            if (count == 0) {
                return;
            }

            // Generate a list of venues.
            List<Element> elements = src.getAllElements("shop");
            for (Element element: elements) {
                Venue venue = new HotpepperVenue();
                venue.setName(element.getFirstElement("name")
                        .getContent().toString());
                venue.setAddress(element.getFirstElement("address")
                        .getContent().toString());
                String latStr = element.getFirstElement("lat")
                        .getContent().toString();
                double latitude = Double.parseDouble(latStr);
                venue.setLatitude(latitude);
                String longStr = element.getFirstElement("lng")
                        .getContent().toString();
                double longitude = Double.parseDouble(longStr);
                venue.setLongitude(longitude);
                venue.setUrl(element.getFirstElement("urls")
                        .getFirstElement("pc")
                        .getContent().toString().split("\\?")[0]);
                Calendar rightNow = Calendar.getInstance();
                Date lastUpdated = rightNow.getTime();
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
        if (start + COUNT < total) {
            getVenues(client, prefecture, start + COUNT);
        }
    }

}
