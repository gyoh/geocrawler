package com.netprice.geocrawler.listener;

import com.netprice.geocrawler.CrawlerEvent;
import com.netprice.geocrawler.CrawlerUtils;
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 *
 * @author gyo
 */
public class GeoPlanetListener implements CrawlerListener {

    private static final Logger logger =
            LoggerFactory.getLogger(GeoPlanetListener.class);

    private static final String SCHEME = "http";
    private static final String HOST = "where.yahooapis.com";
    private static final int PORT = 80;
    private static final String PATH = "/v1/places.q";
    private static final String APPID = "RHd4gYDV34EQnv30YDikfICegKRWzwIJK3MxGurjQTaS4gDpIGYE_57prOAJgTfPhmC1";

    @Override
    public void crawlerEvent(CrawlerEvent event) {
        List<Venue> venues = event.getVenues();
        setWOEIDs(venues);
    }

    private void setWOEIDs(List<Venue> venues) {
        // Create an instance of HttpClient.
        HttpClient client = new HttpClient();
        client.getHostConfiguration().setHost(HOST, PORT, SCHEME);

        for (Venue venue: venues) {
            String address = venue.getAddress();
            if (address != null) {
                int woeid = lookupWOEID(client, address);
                venue.setWoeid(woeid);
            }
        }
    }

    private int lookupWOEID(HttpClient client, String address) {
        // Wait for a second to prevent DoS.
        CrawlerUtils.pause(1);

        int woeid = 0;

        String encodedAddress = null;
        try {
            encodedAddress = URLEncoder.encode(address, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error("Unsupported encoding", e);
            return woeid;
        }

        // Create a method instance.
        GetMethod getMethod = new GetMethod(PATH + "(" + encodedAddress + ")");
        getMethod.setQueryString(new NameValuePair[] {
            new NameValuePair("appid", APPID)
        });

        try {
            // Execute the method.
            int statusCode = client.executeMethod(getMethod);
            if (statusCode != HttpStatus.SC_OK) {
                logger.error("Method failed: {}", getMethod.getStatusLine());
                return woeid;
            }

            // Read the response body.
            InputStream responseBodyAsStream =
                    getMethod.getResponseBodyAsStream();
            BufferedInputStream bis =
                new BufferedInputStream(responseBodyAsStream);

            // Deal with the response.
            Source src = new Source(bis);
            src.fullSequentialParse();

            Element woeidElement = src.getFirstElement("woeid");
            if (woeidElement != null) {
                String woeidStr = woeidElement.getContent().toString();
                woeid = Integer.parseInt(woeidStr);
            }

            // Close input streams
            bis.close();
        } catch (HttpException e) {
            logger.error("Fatal protocol violation", e);
        } catch (IOException e) {
            logger.error("Fatal transport error", e);
        } catch (IllegalArgumentException e) {
            logger.error("Illegal or inappropriate argument", e);
        } finally {
            // Release the connection.
            getMethod.releaseConnection();
        }

        logger.debug("{}: {}", woeid, address);
        return woeid;
    }

}
