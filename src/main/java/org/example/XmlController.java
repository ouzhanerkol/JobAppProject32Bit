package org.example;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.example.model.*;
import org.hibernate.HibernateException;
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
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class XmlController {
    private static final Logger LOG = LogManager.getLogger(XmlController.class);

    /**
     * get currencies from given url and
     * persist them to database
     * and creates new xml file after the whole process is finished
     *
     * @param nameOfUrl - the name of url address to provide connection
     * @throws IOException
     */
    public void getCurrenciesFromURL(String nameOfUrl) throws IOException {
        LOG.trace("Entering method getCurrenciesFromURL().");
        // creating url and open connection
        // creating doc and dom source for xml transform
        URL url = new URL(nameOfUrl);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        Document doc = getDocFromUrl(url);
        DOMSource domSource = new DOMSource(doc); // for xml transform

        // set request and url connect
        urlConnection.setRequestMethod("HEAD");
        urlConnection.connect();
        LOG.debug("url connection done.");

        // we need few data from url
        int connectionResponseCode = urlConnection.getResponseCode();
        long lastModified = urlConnection.getLastModified();

        // checking connection first
        if (connectionResponseCode == HttpsURLConnection.HTTP_OK) {
            LOG.debug("HTTP Status-Code 200: OK.");
            // if url updated
            // get currencies from url
            if (!(getLastModified().equals(String.valueOf(lastModified)))) {
                LOG.info("Url modified! PersistCurrencies() starting...");
                persistCurrencies(doc);
                LOG.trace("Setting last modified date.");
                setLastModified(lastModified);
                /*
                After finish commit currencies, transform data
                to new XML file with XSL Transform
                */
                try {
                    XSLTProcessor.transformXMLUsingXSLT(
                            domSource,
                            "stylesheet.xslt",
                            "Rates_" + getYyyyMMdd() + "_" + getHHmmss() + ".xml");
                    LOG.info("XSL Transform done.");
                } catch (TransformerException e) {
                    LOG.error("Failed when transform xml with XSLT. Error: " + e);
                    throw new RuntimeException(e);
                }
            } else {
                LOG.info("URL isn't updated yet...");
            }

        } else {
            LOG.error("URL connection is bad...");
        }
    }

    /**
     * returns document from given url link
     *
     * @param url - the url address to open Stream
     * @return the url document
     */
    public Document getDocFromUrl(URL url) {
        LOG.trace("getting document from url..");
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // an instance of builder to parse the specified xml file
        DocumentBuilder db = null;
        try {
            // parse the xml file to doc and return
            db = dbf.newDocumentBuilder();
            Document doc = db.parse(url.openStream());
            doc.getDocumentElement().normalize();
            LOG.info("Building xml document is successful");
            return doc;
        } catch (ParserConfigurationException | IOException | SAXException e) {
            LOG.error("Exception occured", new Exception("Document building failed.."));
            return null;
        }
    }

    /**
     * persist currencies to database
     *
     * @param doc - the document we obtained from the url
     */
    public void persistCurrencies(Document doc) {
        // find currencies from document with tag name
        // and assign to node list
        NodeList nodeList = doc.getElementsByTagName("Currency");
        LOG.debug("Node list created.");

        // creating entity transaction with persistence unit
        // persistence-unit is name of persistence unit in persistence.xml
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("persistence-unit");
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction entityTransaction = entityManager.getTransaction();
        LOG.debug("Entity manager and Entity created.");

        // processes each node in the list one by one
        for (int itr = 0; itr < nodeList.getLength(); itr++) {
            LOG.debug("Collecting nodes...");
            Node node = nodeList.item(itr);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) node;
                // Start a resource transaction
                LOG.info("Entity transaction begin");
                entityTransaction.begin();

                LOG.debug("Getting elements by tag names...");
                // get all text content with tag name for entity elements
                String currencyCode = eElement.getAttribute("CurrencyCode");
                String unitTextContent = eElement.getElementsByTagName("Unit").item(0).getTextContent();
                String forexBuyingTextContent = eElement.getElementsByTagName("ForexBuying").item(0).getTextContent();
                String forexSellingTextContent = eElement.getElementsByTagName("ForexSelling").item(0).getTextContent();
                String banknoteBuyingTextContent = eElement.getElementsByTagName("BanknoteBuying").item(0).getTextContent();
                String banknoteSellingTextContent = eElement.getElementsByTagName("BanknoteSelling").item(0).getTextContent();
                String crossRateUSDTextContent = eElement.getElementsByTagName("CrossRateUSD").item(0).getTextContent();
                String crossRateOtherTextContent = eElement.getElementsByTagName("CrossRateOther").item(0).getTextContent();

                LOG.trace("Parsing text contents");
                // set parameters for entities
                int unit = tryParseInt(unitTextContent);
                double forexBuying = tryParseDouble(forexBuyingTextContent);

                // For information data we need last node of list
                if (itr == nodeList.getLength() - 1) {
                    LOG.debug("Information persist started.");
                    // Create an instance of the Information and persist here
                    double crossRateOther = Double.parseDouble(crossRateOtherTextContent);
                    Information information = new Information(new Date(), currencyCode, unit, crossRateOther, forexBuying);
                    // persist Information to entity manager
                    entityManager.persist(information);
                    LOG.info("Information persist finished");
                } else {
                    // getting forexSelling from node
                    // Create an instance of the Forex and persist here
                    double forexSelling = Double.parseDouble(forexSellingTextContent);
                    Forex forex = new Forex(new Date(), currencyCode, unit, forexBuying, forexSelling);
                    entityManager.persist(forex);
                    LOG.info("Forex persist finished");
                    /*
                      There is two different cross rate in xml
                      So need to check which one is null
                      CrossRateUSD is holding USD/{CurrencyCode}
                      CrossRateOther is holding {CurrencyCode}/USD
                     */
                    if (!crossRateUSDTextContent.equals("")) {
                        // Create an instance of the crossRates with CrossRateUSD
                        double crossRateUSD = Double.parseDouble(crossRateUSDTextContent);
                        CrossRates crossRates = new CrossRates(new Date(), currencyCode, unit, crossRateUSD);
                        entityManager.persist(crossRates);
                        LOG.info("Cross rate USD persist finished");
                    } else if (!crossRateOtherTextContent.equals("")) {
                        // Create an instance of the crossRates with CrossRateOther
                        double crossRateOther = Double.parseDouble(crossRateOtherTextContent);
                        CrossRates crossRates = new CrossRates(new Date(), currencyCode, unit, crossRateOther);
                        entityManager.persist(crossRates);
                        LOG.info("Cross rate Other persist finished");
                    }

                    // Because of some banknotes can be null, checking contents first
                    // If null, persist without banknote buying and banknote selling
                    if (banknoteBuyingTextContent.equals("") || banknoteSellingTextContent.equals("")) {
                        Banknote banknote = new Banknote(new Date(), currencyCode, unit);
                        entityManager.persist(banknote);
                        LOG.info("Banknote without buying and selling persist finished");
                    } else {
                        // get contents and parse them
                        // persist with banknote buying and banknote selling
                        double banknoteBuying = Double.parseDouble(banknoteBuyingTextContent);
                        double banknoteSelling = Double.parseDouble(banknoteSellingTextContent);
                        Banknote banknote = new Banknote(new Date(), currencyCode, unit, banknoteBuying, banknoteSelling);
                        entityManager.persist(banknote);
                        LOG.info("Banknote with buying and selling persist finished");
                    }
                }
                // committing transaction
                // if throws exception than rollback the previous changes
                try {
                    entityTransaction.commit();
                    LOG.info("Currencies added successfully");

                } catch (HibernateException he) {
                    LOG.error("Adding Currencies failed!");
                    entityTransaction.rollback();
                }
            }
        }
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

    /**
     * updates the last modified variable in the config.properties file
     *
     * @param lastModified - last modified variable from the url link
     */
    public static void setLastModified(long lastModified) {
        LOG.trace("setting last modified date..");
        PropertiesConfiguration config = null;
        try {
            config = new PropertiesConfiguration("config.properties");
            LOG.debug("configuration completed.");
        } catch (ConfigurationException e) {
            LOG.error("configuration failed.");
            throw new RuntimeException(e);
        }
        config.setProperty("last.modified", String.valueOf(lastModified));
        try {
            config.save();
            LOG.info("config saved.");
        } catch (ConfigurationException e) {
            LOG.error("config save failed.");
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the value of the variable named last.modified in config.properties
     *
     * @return the value of last.modified in the config file as String
     */
    public static String getLastModified() {
        LOG.trace("getting last modified date..");
        PropertiesConfiguration config = null;
        try {
            config = new PropertiesConfiguration("config.properties");
            LOG.debug("configuration completed.");
        } catch (ConfigurationException e) {
            LOG.error("configuration failed.");
            throw new RuntimeException(e);
        }
        return config.getString("last.modified");
    }

    /**
     * A simple method to avoid problems with Exception
     * when converting the given text to integer
     *
     * @param text - Text to be converted to integer
     * @return Integer converted from text
     */
    public static Integer tryParseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            LOG.error("Failed when try parse " + text + " to int!");
            return null;
        }
    }

    /**
     * A simple method to avoid problems with Exception
     * when converting the given text to double
     *
     * @param text - Text to be converted to double
     * @return Double converted from text
     */
    public static Double tryParseDouble(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            LOG.error("Failed when try parse " + text + " to double!");
            return null;
        }
    }
}