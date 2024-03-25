package org.example;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Config {

    private static final String CONFIG_FILE = "config.properties";
    private static final PropertiesConfiguration config;

    private static final Logger LOG = LogManager.getLogger(XmlController.class);


    static {
        try {
            config = new PropertiesConfiguration(CONFIG_FILE);
        } catch (ConfigurationException e) {
            throw new RuntimeException("Failed to load configuration file: " + CONFIG_FILE, e);
        }
    }

    public static String getLastModified() {
        return config.getString("last.modified");
    }

    public static void setLastModified(long lastModified) {
        LOG.trace("Setting last modified date...");
        config.setProperty("last.modified", String.valueOf(lastModified));
        try {
            config.save();
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

}

