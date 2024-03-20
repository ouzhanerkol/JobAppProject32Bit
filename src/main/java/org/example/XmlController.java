package org.example;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
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
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class XmlController {
    private static final Logger LOG = LogManager.getLogger(XmlController.class);

    /**
     * Connects to a given URL link
     * @param nameOfUrl - the name of url address to provide connection
     * @throws IOException
     */
    public void connectURL(String nameOfUrl) throws IOException {
        URL url = new URL(nameOfUrl);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();

        //set request and url connect
        urlConnection.setRequestMethod("HEAD");
        urlConnection.connect();

        //we need few data from url
        int connectionResponseCode = urlConnection.getResponseCode();
        long lastModified = urlConnection.getLastModified();

        // checking connection first
        if (connectionResponseCode == HttpsURLConnection.HTTP_OK) {
            // if url updated
            // get currencies from url
            if (!(getLastModified().equals(String.valueOf(lastModified)))) {
                getCurrenciesFromURL(url);
                setLastModified(lastModified);
            } else {
                LOG.info("URL isn't updated yet...");
            }
        } else {
            LOG.error("URL connection is bad...");
        }
    }

    /**
     * returns the currency list from the xml file in a given url link
     * @param url - the url address to open Stream
     * @return the currency list
     */
    public NodeList getCurrencyNodeList(URL url) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        //an instance of builder to parse the specified xml file
        DocumentBuilder db = null;
        try {
            // parse the xml file to doc and return the currency list
            db = dbf.newDocumentBuilder();
            Document doc = db.parse(url.openStream());
            doc.getDocumentElement().normalize();
            LOG.info("getting Currencies is successful");

            return doc.getElementsByTagName("Currency");
        } catch (ParserConfigurationException | IOException | SAXException e) {
            LOG.error("Exception occured", new Exception("Document building failed.."));
            return null;
        }
    }

    /**
     * get currencies from URL and writes them to database
     * and creates new xml file after the whole process is finished
     * @param url - source URL
     */
    public void getCurrenciesFromURL(URL url) {
        // get currencies with node list from URL
        NodeList nodeList = getCurrencyNodeList(url);

        // creating entity transaction with persistence unit
        // persistence-unit is name of persistence unit in persistence.xml
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("persistence-unit");
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction entityTransaction = entityManager.getTransaction();

        // processes each node in the list one by one
        for (int itr = 0; itr < nodeList.getLength(); itr++) {
            Node node = nodeList.item(itr);
            LOG.info("Collecting tags...");
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) node;
                // Start a resource transaction
                entityTransaction.begin();

                // get elements with tag name for CurrencyCode, Unit and ForexBuying
                String currencyCode = eElement.getAttribute("CurrencyCode");
                int unit = Integer.parseInt(eElement.getElementsByTagName("Unit").item(0).getTextContent());
                double forexBuying = Double.parseDouble(eElement.getElementsByTagName("ForexBuying").item(0).getTextContent());

                if (itr == nodeList.getLength() - 1) {
                    double crossRateOther = Double.parseDouble(eElement.getElementsByTagName("CrossRateOther").item(0).getTextContent());
                    Information information = new Information(new Date(), currencyCode, unit, crossRateOther, forexBuying);
                    // persist Information to entity manager
                    entityManager.persist(information);
                    LOG.info("Information finished");
                } else {
                    double forexSelling = Double.parseDouble(eElement.getElementsByTagName("ForexSelling").item(0).getTextContent());
                    Forex forex = new Forex(new Date(), currencyCode, unit, forexBuying, forexSelling);
                    entityManager.persist(forex);


                    if (!eElement.getElementsByTagName("CrossRateUSD").item(0).getTextContent().equals("")) {
                        double crossRateOther = Double.parseDouble(eElement.getElementsByTagName("CrossRateUSD").item(0).getTextContent());

                        CrossRates crossRates = new CrossRates(new Date(), currencyCode, unit, crossRateOther);
                        entityManager.persist(crossRates);

                    } else if (!eElement.getElementsByTagName("CrossRateOther").item(0).getTextContent().equals("")) {
                        double crossRateUSD = Double.parseDouble(eElement.getElementsByTagName("CrossRateOther").item(0).getTextContent());
                        CrossRates crossRates = new CrossRates(new Date(), currencyCode, unit, crossRateUSD);
                        entityManager.persist(crossRates);
                    }

                    if (eElement.getElementsByTagName("BanknoteBuying").item(0).getTextContent().equals("") || eElement.getElementsByTagName("BanknoteSelling").item(0).getTextContent().equals("")) {
                        Banknote banknote = new Banknote(new Date(), currencyCode, unit);
                        entityManager.persist(banknote);

                    } else {
                        double banknoteBuying = Double.parseDouble(eElement.getElementsByTagName("BanknoteBuying").item(0).getTextContent());
                        double banknoteSelling = Double.parseDouble(eElement.getElementsByTagName("BanknoteSelling").item(0).getTextContent());
                        Banknote banknote = new Banknote(new Date(), currencyCode, unit, banknoteBuying, banknoteSelling);
                        entityManager.persist(banknote);
                    }
                }
                entityTransaction.commit();
                LOG.info("Currencies added successfully");
            }
        }
        try {
            XSLTProcessor.transformXMLUsingXSLT(
                    "input.xml", // TODO: change this part
                    "stylesheet.xslt",
                    "Rates_" + getYyyyMMdd() + "_" + getHHmmss() + ".xml");
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates string in yyyyMMdd format for XML naming
     * @return string in Date format yyyyMMdd
     */
    private String getYyyyMMdd() {
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
        return f.format(new Date());
    }

    /**
     * Generates string in HHmmss format for XML naming
     * @return string in Date format HHmmss
     */
    private String getHHmmss() {
        SimpleDateFormat f = new SimpleDateFormat("HHmmss");
        return f.format(new Date());
    }

    /**
     * updates the last modified variable in the config.properties file
     * @param lastModified - last modified variable from the url link
     */
    public static void setLastModified(long lastModified) {
        PropertiesConfiguration config = null;
        try {
            config = new PropertiesConfiguration("config.properties");
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
        config.setProperty("last.modified", String.valueOf(lastModified));
        try {
            config.save();
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the value of the variable named last.modified in config.properties
     * @return the value of last.modified in the config file as String
     * @throws IOException
     */
    public static String getLastModified() throws IOException {
        PropertiesConfiguration config = null;
        try {
            config = new PropertiesConfiguration("config.properties");
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
        return config.getString("last.modified");
    }
}