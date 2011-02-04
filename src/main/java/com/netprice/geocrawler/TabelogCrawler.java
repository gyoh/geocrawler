package com.netprice.geocrawler;

import com.netprice.geocrawler.listener.CrawlerListener;
import com.netprice.geocrawler.manager.PropertyManager;
import com.netprice.geocrawler.model.TabelogVenue;
import com.netprice.geocrawler.model.Venue;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
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
 * Created by IntelliJ IDEA.
 * User: gyo
 * Date: Nov 5, 2010
 * Time: 11:47:30 AM
 */
public class TabelogCrawler extends Crawler {

    private static final Logger logger =
            LoggerFactory.getLogger(TabelogCrawler.class);

    private static final String SCHEME = "http";
    private static final String HOST = "r.tabelog.com";
    private static final int PORT = 80;
    private static final String PATH = "/sitemap/";
    private static final String SERVER = SCHEME + "://" + HOST;
    private static final int COUNT = 200;    

    protected void fetch() {
        // Create an instance of HttpClient.
        HttpClient client = new HttpClient();
        client.getHostConfiguration().setHost(HOST, PORT, SCHEME);

        List<String> prefPaths = getPaths(client, PATH);
        for (String prefPath: prefPaths) {
            logger.debug("Prefecture path: {}", prefPath);
            List<String> areaPaths = getPaths(client, prefPath);
            for (String areaPath: areaPaths) {
                logger.debug("Area path: {}", areaPath);
                List<String> namePaths = getPaths(client, areaPath);
                for (String namePath: namePaths) {
                    logger.debug("Name path: {}", namePath);
                    getVenues(client, namePath, 1);
                }
            }
        }
    }

    private List<String> getPaths(HttpClient client, String path) {
        // Create a method instance.
        GetMethod getMethod = new GetMethod(path);
        getMethod.addRequestHeader("User-Agent",
                PropertyManager.getInstance().getUserAgent());
        getMethod.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);

        List<String> paths = new ArrayList<String>();

        try {
            // Execute the method.
            int statusCode = client.executeMethod(getMethod);
            if (statusCode != HttpStatus.SC_OK) {
                logger.error("Method failed: {}", getMethod.getStatusLine());
                return paths;
            }

            // Read the response body.
            InputStream responseBodyAsStream =
                    getMethod.getResponseBodyAsStream();
            BufferedInputStream bis =
                new BufferedInputStream(responseBodyAsStream);

            // Deal with the response.
            Source src = new Source(bis);
            src.fullSequentialParse();

            Element arealst = src.getElementById("arealst_sitemap");
            List<Element> lists = arealst.getAllElements(HTMLElementName.LI);
            for (Element list: lists) {
                Element link = list.getFirstElement(HTMLElementName.A);
                if (link != null) {
                    paths.add(link.getAttributeValue("href"));
                }
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

        return paths;
    }

    private void getVenues(HttpClient client, String path, int pageId) {
        logger.info("Page " + pageId);

        // Create a method instance.
        GetMethod getMethod = new GetMethod(path);
        getMethod.addRequestHeader("User-Agent",
                PropertyManager.getInstance().getUserAgent());
        getMethod.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        getMethod.setQueryString(new NameValuePair[] {
            new NameValuePair("PG", String.valueOf(pageId))
        });

        // CrawlerEvent holds the list of venues to pass to the listeners.
        List<Venue> venues = new ArrayList<Venue>();
        CrawlerEvent event = new CrawlerEvent();
        event.setVenues(venues);
        event.setType(EventType.TABELOG);

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
            Element rstlst = src.getElementById("rstlst_sitemap");

            // Get the total count.
            Element totalElement = rstlst.getFirstElementByClass("result_num");
            String totalStr = totalElement.getFirstElement("strong")
                    .getContent().toString();
            total = Integer.parseInt(totalStr);
            logger.info("total venues: {}", total);

            // Generate a list of venues.
            List<Element> elements = rstlst.getAllElementsByClass("rstname");
            for (Element element: elements) {
                String venuePath = element.getFirstElement(HTMLElementName.A)
                        .getAttributeValue("href");
                Venue venue = getVenue(client, venuePath);
                if (venue != null) {
                    venues.add(venue);
                }
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
        if (COUNT * pageId < total) {
            getVenues(client, path, ++pageId);
        }
    }

    private Venue getVenue(HttpClient client, String path) {
        // Wait for a second to prevent DoS.
        CrawlerUtils.pause(1);

        // Create a method instance.
        GetMethod getMethod = new GetMethod(path);
        getMethod.addRequestHeader("User-Agent",
                PropertyManager.getInstance().getUserAgent());
        getMethod.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);

        Venue venue = new TabelogVenue();
        try {
            // Execute the method.
            int statusCode = client.executeMethod(getMethod);
            if (statusCode != HttpStatus.SC_OK) {
                logger.error("Method failed: {}", getMethod.getStatusLine());
                return null;
            }

            // Read the response body.
            InputStream responseBodyAsStream =
                    getMethod.getResponseBodyAsStream();
            BufferedInputStream bis =
                new BufferedInputStream(responseBodyAsStream);

            // Deal with the response.
            Source src = new Source(bis);
            src.fullSequentialParse();

            Element rstData = src.getFirstElementByClass("rst-data");
            Element rstName = rstData.getFirstElementByClass("rst-name");
            venue.setName(rstName.getFirstElement(HTMLElementName.STRONG)
                    .getContent().toString());

            String[] values = {"v:region", "v:locality", "v:street-address"};
            StringBuilder address = new StringBuilder();
            for (String value : values) {
                Element element = rstData.getFirstElement(
                        "property", value, false);
                if (element != null) {
                    address.append(element.getContent().
                            getTextExtractor().toString());
                }
            }
            venue.setAddress(address.toString());

            Element rstMap = src.getFirstElementByClass("rst-map");
            Element map = rstMap.getFirstElement(HTMLElementName.IMG);
            String mapLink = map.getAttributeValue("src");
            String markers = CrawlerUtils.getParameter(mapLink, "markers");
            if (markers != null) {
                String[] latlong = markers.split(",");
                venue.setLatitude(Double.parseDouble(latlong[0]));
                venue.setLongitude(Double.parseDouble(latlong[1]));
            }
            venue.setUrl(SERVER + path);
            Calendar rightNow = Calendar.getInstance();
            Date lastUpdated = rightNow.getTime();
            venue.setLastUpdated(lastUpdated);            
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

        return venue;
    }

}
