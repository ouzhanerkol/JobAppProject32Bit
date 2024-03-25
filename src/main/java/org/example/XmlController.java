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

    private void processCurrencies(URL url, long lastModified) {
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

    private void validateUrl(String urlString) {
        if (urlString == null || urlString.isEmpty()) {
            throw new IllegalArgumentException("urlString cannot be null or empty");
        }
    }

    /**
     * Retrieves a document object from the provided URL.
     *
     * @param url - the url address to open Stream
     * @return the parsed document object
     * @throws IOException if an IO error occurs
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

    private void persistCurrencies(EntityManager entityManager, Document doc) {
        // Find currencies from document with tag name and
        // assign to node list
        NodeList nodeList = doc.getElementsByTagName("Currency");
        LOG.debug("Node list created.");

        // processes each node in the list one by one
        for (int itr = 0; itr < nodeList.getLength(); itr++) {
            LOG.debug("Collecting nodes...");
            Node node = nodeList.item(itr);

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
                if (itr == nodeList.getLength() - 1) {
                    // Create an instance of the Information and
                    // insert to the database
                    LOG.debug("Information persist starting...");
                    persistInformation(entityManager, currencyCode, unit, currencyMap);

                } else {
                    // Create an instance of the Forex and
                    // insert to the database
                    persistForex(entityManager, currencyCode, unit, currencyMap);
                    persistBanknote(entityManager, currencyCode, unit, currencyMap);
                    if (itr != 0) {
                        persistCrossRate(entityManager, currencyCode, unit, currencyMap);
                    }
                }
            }
        }
    }

    private void persistCrossRate(EntityManager entityManager, String currencyCode, int unit, Map<String, Double> currencyMap) {
        LOG.debug("Cross Rate persist starting...");
        double crossRate = currencyMap.containsKey("CrossRateUSD") ?
                currencyMap.get("CrossRateUSD") : currencyMap.get("CrossRateOther");
        CrossRates crossRates = new CrossRates(new Date(), currencyCode, unit, crossRate);
        entityManager.persist(crossRates);
        LOG.info("Cross rate persist finished.");
    }

    private void persistBanknote(EntityManager entityManager, String currencyCode, int unit, Map<String, Double> currencyMap) {
        LOG.debug("Banknote persist starting...");
        double buying = currencyMap.getOrDefault("BanknoteBuying", 0.0);
        double selling = currencyMap.getOrDefault("BanknoteSelling", 0.0);
        Banknote banknote = new Banknote(new Date(), currencyCode, unit, buying, selling);
        entityManager.persist(banknote);
        LOG.info("Banknote persist finished");
    }

    private void persistForex(EntityManager entityManager, String currencyCode, int unit, Map<String, Double> currencyMap) {
        // Implement logic to persist Forex entity based on currencyMap values
        LOG.debug("Forex persist starting...");
        Forex forex = new Forex(new Date(), currencyCode, unit, currencyMap.get("ForexBuying"), currencyMap.get("ForexSelling"));
        entityManager.persist(forex);
        LOG.info("Forex persist finished");
    }

    private void persistInformation(EntityManager entityManager, String currencyCode, int unit, Map<String, Double> currencyMap) {
        // Implement logic to persist Information entity based on currencyMap values
        LOG.debug("Information persist starting...");
        Information information = new Information(new Date(), currencyCode, unit, currencyMap.get("CrossRateOther"), currencyMap.get("ForexBuying"));
        entityManager.persist(information);
        LOG.info("Information persist finished");
    }

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
     * @return cm - Map list key - tag name, value - got by tag name
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
        LOG.trace("getting string in yyyyMMdd format..");
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
        return f.format(new Date());
    }

    /**
     * Generates string in HHmmss format for XML naming
     *
     * @return string in Date format HHmmss
     */
    private String getHHmmss() {
        LOG.trace("getting string in HHmmss format..");
        SimpleDateFormat f = new SimpleDateFormat("HHmmss");
        return f.format(new Date());
    }
}