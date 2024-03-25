package org.example;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.example.model.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.net.ssl.HttpsURLConnection;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class XmlController {
    private static final Logger LOG = LogManager.getLogger(XmlController.class);

    public static final String[] CURRENCY_TAG_NAMES = {
            "ForexBuying",
            "ForexSelling",
            "BanknoteBuying",
            "BanknoteSelling",
            "CrossRateUSD",
            "CrossRateOther"
    };

    /**
     * Fetches currencies from a given URL,
     * persists them to the database,
     * and creates a new transformed XML file.
     *
     * @param urlString - the URL string to connect to
     * @throws IOException - if an IO error occurs
     */
    public void getCurrenciesFromURL(String urlString) throws IOException {
        LOG.info("Starting getCurrenciesFromURL with URL: " + urlString);
        long startTime = System.currentTimeMillis();

        // Validate URL
        validateUrl(urlString);

        // Connection logic with retries and backoff
        int maxRetries = 3; // Set the maximum number of retries
        int retryCount = 0;
        long backoffMillis = 1000; // Initial backoff time in milliseconds
        HttpsURLConnection urlConnection = null;

        while (retryCount < maxRetries) {
            try {
                URL url = new URL(urlString);
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestMethod("HEAD");
                urlConnection.connect();

                int connectionResponseCode = urlConnection.getResponseCode();
                long lastModified = urlConnection.getLastModified();

                // Check for successful connection (consider specific response codes)
                if (connectionResponseCode == HttpsURLConnection.HTTP_OK) {
                    // Successful connection, process data
                    LOG.info("Connection successful on attempt: " + (retryCount + 1));
                    processCurrencies(url, lastModified);

                    break; // Exit loop on success
                } else {
                    throw new IOException("Connection failed with status code: " + urlConnection.getResponseCode());
                }

            } catch (IOException e) {
                retryCount++;
                LOG.warn("Connection failed. Retry attempt: " + retryCount + ". Error: " + e.getMessage());

                // Implement exponential backoff with jitter
                backoffMillis *= 2; // Double the backoff time
                backoffMillis += Math.random() * backoffMillis; // Add random jitter

                try {
                    Thread.sleep(backoffMillis); // Sleep for calculated duration
                } catch (InterruptedException e1) {
                    LOG.warn("Thread interrupted during backoff: " + e1.getMessage());
                    // Handle interruption (optional)
                }
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                    long endTime = System.currentTimeMillis();
                    LOG.info("getCurrenciesFromURL() completed in " + (endTime - startTime) + " ms.");
                }
            }
        }

        if (retryCount == maxRetries) {
            throw new IOException("Failed to connect to URL after " + maxRetries + " retries");
        }
    }

    /**
     * Processes the retrieved currencies from the provided URL.
     * This includes checking for updates, committing data to the database,
     * and generating a transformed XML file.
     *
     * @param url - the URL object representing the currency data source
     * @param lastModified - the last modified timestamp of the remote resource
     * @throws RuntimeException - if an unexpected error occurs during processing
     */
    private void processCurrencies(URL url, long lastModified) throws RuntimeException {
        LOG.info("Starting processCurrencies with URL: " + url);
        Document doc = null;
        try {
            doc = getDocFromUrl(url);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String newLastModified = String.valueOf(lastModified);

        if (Config.getLastModified().equals(newLastModified)) {
            LOG.info("URL isn't updated yet...");
            return;
        }

        LOG.info("Url modified! commitCurrencies() starting...");
        commitCurrencies(doc);
        Config.setLastModified(lastModified);

        // After finish commit currencies, transform data
        // to new XML file with XSL Transform
        try {
            XSLTProcessor.transformXMLUsingXSLT(new DOMSource(doc), "stylesheet.xslt",
                    "Rates_" + getYyyyMMdd() + "_" + getHHmmss() + ".xml");
        } catch (TransformerException e) {
            LOG.error("Failed to transform XML with XSLT. Error: " + e);
            throw new RuntimeException(e);
        }
        LOG.info("Getting currencies is completed...");
    }

    /**
     * Validates the provided URL string.
     * Checks for null or empty strings.
     *
     * @param urlString - the URL string to validate
     * @throws IllegalArgumentException - if the URL string is null or empty
     */
    private void validateUrl(String urlString) {
        if (urlString == null || urlString.isEmpty()) {
            throw new IllegalArgumentException("urlString cannot be null or empty");
        }
    }

    /**
     * Retrieves a document object from the provided URL.
     * Handles potential exceptions during document parsing and throws appropriate exceptions.
     *
     * @param url - the url address to open Stream
     * @return the parsed document object
     * @throws IOException - if an IO error occurs
     */
    public Document getDocFromUrl(URL url) throws IOException {
        LOG.trace("getting document from url..");
        try (InputStream stream = url.openStream()) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(stream);
            doc.getDocumentElement().normalize();
            return doc;
        } catch (ParserConfigurationException | SAXException e) {
            LOG.error("Exception occurred during document building: " + e.getMessage());
            throw new IOException("Failed to build document from URL", e); // Wrap with IOException
        } catch (NoSuchElementException e) {
            LOG.error("Missing element in the document: " + e.getMessage());
            throw new RuntimeException("Missing element in the document", e);
        }
    }

    /**
     * Commits currencies extracted from the document to the database.
     *
     * @param doc - the document containing currency data
     */
    public void commitCurrencies(Document doc) {
        // Creating entity transaction with persistence unit
        // Persistence-unit is name of persistence unit in persistence.xml
        LOG.debug("Entity transaction begin...");
        EntityManagerFactory entityManagerFactory = null;
        EntityManager entityManager = null;
        EntityTransaction entityTransaction = null;
        try {
            entityManagerFactory = Persistence.createEntityManagerFactory("persistence-unit");
            entityManager = entityManagerFactory.createEntityManager();
            entityTransaction = entityManager.getTransaction();
            entityTransaction.begin();
            LOG.info("Entity transaction began!");

            persistCurrencies(entityManager, doc);
            entityTransaction.commit();
            LOG.info("Currencies added successfully");
        } catch (Exception e) {
            LOG.error("Adding Currencies failed!", e);
            if (entityTransaction != null && entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
            if (entityManagerFactory != null) {
                entityManagerFactory.close();
            }
            LOG.info("Entity manager and factory closed.");
        }
    }

    /**
     * Persists individual currency entries from the document to the database.
     * This method used internally by `commitCurrencies`.
     *
     * @param entityManager - the EntityManager instance for interacting with the database
     * @param doc - the document containing the parsed currency data
     */
    private void persistCurrencies(EntityManager entityManager, Document doc) {
        LOG.info("Starting persist currencies...");
        // Find currencies from document with tag name and
        // assign to node list
        NodeList nodeList = doc.getElementsByTagName("Currency");
        LOG.debug("Node list created.");

        // processes each node in the list one by one
        for (int nodeIndex = 0; nodeIndex < nodeList.getLength(); nodeIndex++) {
            LOG.debug("Collecting nodes...");
            Node node = nodeList.item(nodeIndex);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                // for getting element by tag names
                // we need to be converting our node to element
                Element element = (Element) node;

                String currencyCode = element.getAttribute("CurrencyCode");
                int unit = parseIntUnit(element);
                Map<String, Double> currencyMap = null;
                try {
                    currencyMap = getListByTagNames(element);
                } catch (CurrencyDataParseException e) {
                    throw new RuntimeException(e);
                }

                // For information data we need last node of list
                if (nodeIndex == nodeList.getLength() - 1) {
                    // Create an instance of the Information and
                    // insert to the database
                    persistInformation(entityManager, currencyCode, unit, currencyMap);

                } else {
                    // Create an instance of the Forex and
                    // insert to the database
                    persistForex(entityManager, currencyCode, unit, currencyMap);
                    persistBanknote(entityManager, currencyCode, unit, currencyMap);
                    if (nodeIndex != 0) {
                        persistCrossRate(entityManager, currencyCode, unit, currencyMap);
                    }
                }
            }
        }
    }

    /**
     * Persists a cross rate entry to the database.
     * Extracts the cross rate value from the provided map based on either "CrossRateUSD" or "CrossRateOther" key.
     * Creates a new `CrossRates` entity object with the extracted data and persists it using the EntityManager.
     *
     * @param entityManager - the EntityManager instance for interacting with the database
     * @param currencyCode - the currency code for the cross rate
     * @param unit - the unit value for the cross rate (e.g., 1, 100, etc.)
     * @param currencyMap - the map containing potential currency data (including cross rates)
     */
    private void persistCrossRate(EntityManager entityManager, String currencyCode, int unit, Map<String, Double> currencyMap) {
        LOG.debug("Cross Rate persist starting...");
        double crossRate = currencyMap.containsKey("CrossRateUSD") ?
                currencyMap.get("CrossRateUSD") : currencyMap.get("CrossRateOther");
        CrossRates crossRates = new CrossRates(new Date(), currencyCode, unit, crossRate);
        entityManager.persist(crossRates);
        LOG.info("Cross rate persist finished.");
    }

    /**
     * Persists a banknote entry to the database.
     * Extracts buying and selling rates for the banknote from the provided map
     * using "BanknoteBuying" and "BanknoteSelling" keys with default values (0.0) if not present.
     * Creates a new `Banknote` entity object with the extracted data and persists it using the EntityManager.
     *
     * @param entityManager - the EntityManager instance for interacting with the database
     * @param currencyCode - the currency code for the banknote
     * @param unit - the unit value for the banknote (e.g., 1, 100, etc.)
     * @param currencyMap - the map containing potential currency data (including banknote rates)
     */
    private void persistBanknote(EntityManager entityManager, String currencyCode, int unit, Map<String, Double> currencyMap) {
        LOG.debug("Banknote persist starting...");
        double buying = currencyMap.getOrDefault("BanknoteBuying", 0.0);
        double selling = currencyMap.getOrDefault("BanknoteSelling", 0.0);
        Banknote banknote = new Banknote(new Date(), currencyCode, unit, buying, selling);
        entityManager.persist(banknote);
        LOG.info("Banknote persist finished");
    }

    /**
     * Persists a forex entry to the database.
     * Extracts buying and selling rates for the forex data from the provided map
     * using "ForexBuying" and "ForexSelling" keys.
     * Creates a new `Forex` entity object with the extracted data and persists it using the EntityManager.
     *
     * @param entityManager - the EntityManager instance for interacting with the database
     * @param currencyCode - the currency code for the forex data
     * @param unit - the unit value for the forex data (e.g., 1, 100, etc.)
     * @param currencyMap - the map containing potential currency data (including forex rates)
     */
    private void persistForex(EntityManager entityManager, String currencyCode, int unit, Map<String, Double> currencyMap) {
        // Implement logic to persist Forex entity based on currencyMap values
        LOG.debug("Forex persist starting...");
        Forex forex = new Forex(new Date(), currencyCode, unit, currencyMap.get("ForexBuying"), currencyMap.get("ForexSelling"));
        entityManager.persist(forex);
        LOG.info("Forex persist finished");
    }

    /**
     * Persists an information entry to the database.
     * Extracts cross rate (from "CrossRateOther") and forex buying rate (from "ForexBuying") from the provided map.
     * Creates a new `Information` entity object with the extracted data and persists it using the EntityManager.
     *
     * @param entityManager - the EntityManager instance for interacting with the database
     * @param currencyCode - the currency code for the information entry
     * @param unit - the unit value for the information data (e.g., 1, 100, etc.)
     * @param currencyMap - the map containing potential currency data (including cross rate and forex buying)
     */
    private void persistInformation(EntityManager entityManager, String currencyCode, int unit, Map<String, Double> currencyMap) {
        // Implement logic to persist Information entity based on currencyMap values
        LOG.debug("Information persist starting...");
        Information information = new Information(new Date(), currencyCode, unit, currencyMap.get("CrossRateOther"), currencyMap.get("ForexBuying"));
        entityManager.persist(information);
        LOG.info("Information persist finished");
    }

    /**
     * Parses the unit value from the provided XML element named "Unit".
     * Handles potential `NumberFormatException` and logs an error message.
     * Defaults the unit value to 1 if parsing fails.
     *
     * @param element - the XML element containing the unit information
     * @return the parsed unit value as an integer (defaults to 1)
     */
    private int parseIntUnit(Element element) {
        int unit = 1;
        try {
            unit = Integer.parseInt(element.getElementsByTagName("Unit").item(0).getTextContent());
        } catch (NumberFormatException e) {
            LOG.error("Failed to parse unit to int!", e);
        }
        return unit;
    }

    /**
     * Generate Map list from element by tag names
     *
     * @param element - An element containing elements by Currency Tag Name
     * @return currencyMap - Map list key - tag name, value - got by tag name
     */
    private Map<String, Double> getListByTagNames(Element element) throws CurrencyDataParseException {
        Map<String, Double> currencyMap = new HashMap<>();

        for (String tagName : CURRENCY_TAG_NAMES) {
            Node tagNode = element.getElementsByTagName(tagName).item(0);
            if (tagNode != null && !tagNode.getTextContent().isEmpty()) {
                try {
                    currencyMap.put(tagName, Double.parseDouble(tagNode.getTextContent()));
                    LOG.debug("Trying put " + tagName + " to map list done");
                } catch (NumberFormatException e) {
                    currencyMap.put(tagName, 0.0);
                    LOG.error("Failed parsing '" + tagName + "' to double: " + e.getMessage());
                    throw new CurrencyDataParseException("Error parsing currency data for tag: " + tagName, e);
                }
            }
        }
        return currencyMap;
    }

    /**
     * Generates string in yyyyMMdd format for XML naming
     *
     * @return string in Date format yyyyMMdd
     */
    private String getYyyyMMdd() {
        return new SimpleDateFormat("yyyyMMdd").format(new Date());
    }

    /**
     * Generates string in HHmmss format for XML naming
     *
     * @return string in Date format HHmmss
     */
    private String getHHmmss() {
        return new SimpleDateFormat("HHmmss").format(new Date());
    }
}