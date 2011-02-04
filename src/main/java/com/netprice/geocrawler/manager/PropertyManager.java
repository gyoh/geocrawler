package com.netprice.geocrawler.manager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyManager {

    private static final String DEFAULT_USER_AGENT = "netprice geocrawler";

    private static final Logger logger = LoggerFactory
            .getLogger(PropertyManager.class);

    private static final PropertyManager INSTANCE = new PropertyManager();

    private final String userAgent;

    public PropertyManager() {
        Properties props = new Properties(System.getProperties());
        String resourceName = "geocrawler.properties";

        try {
            InputStream inputStream = this.getClass().getClassLoader()
                    .getResourceAsStream(resourceName);
            if (inputStream == null) {
                inputStream = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream(resourceName);
            }
            if (inputStream != null) {
                props.load(inputStream);
                System.getProperties().putAll(props);
                inputStream.close();
            } else {
                logger.error("Could not find {} on classpath", resourceName);
            }
        } catch (IOException e) {
            // do nothing - we don't want to fail
            // just because we could not find the resource
            logger.error("Error reading {} from classpath: {}", resourceName, e
                    .getMessage());
        }

        userAgent = System.getProperty("com.netprice.geocrawler.userAgent",
                DEFAULT_USER_AGENT);
    }

    public static PropertyManager getInstance() {
        return INSTANCE;
    }

    public String getUserAgent() {
        return userAgent;
    }

}
