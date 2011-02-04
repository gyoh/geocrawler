package com.netprice.geocrawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CrawlerUtils {

    private static final Logger logger =
            LoggerFactory.getLogger(CrawlerUtils.class);

    public static String getParameter(String URL, String key) {
        String param = null;
        try {
            URL url = new URL(URL);
            String query = url.getQuery();
            Map<String, String> queryMap = getQueryMap(query);
            param = queryMap.get(key);
        } catch (MalformedURLException e) {
            logger.error("Invalid URL", e);
        }
        return param;
    }

    public static Map<String, String> getQueryMap(String query) {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }
        return map;
    }

    public static void pause (int n) {
        long t0, t1;
        t0 =  System.currentTimeMillis();
        do {
            t1 = System.currentTimeMillis();
        }
        while ((t1 - t0) < (n * 1000));
    }

}
