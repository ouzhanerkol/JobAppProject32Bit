package org.example;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.example.entities.Banknote;
import org.example.entities.CrossRates;
import org.example.entities.Forex;
import org.example.entities.Information;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.net.ssl.HttpsURLConnection;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;
import java.util.Date;

public class Quartz implements Job {
    private Logger log = Logger.getLogger(Quartz.class);

    private static final String NAME_OF_URL = "https://www.tcmb.gov.tr/kurlar/today.xml";

    public void execute(JobExecutionContext jExeCtx) throws JobExecutionException {//handle JobExecutionException

        //debug message
        log.debug(new Date() + " Quartz is running......");
        try {
            connectURL();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void connectURL() throws IOException {
        URL url = new URL(NAME_OF_URL);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.connect();

        int connectionResponseCode = urlConnection.getResponseCode();
        long lastModified = urlConnection.getLastModified();

        if (connectionResponseCode == HttpsURLConnection.HTTP_OK) {
            if (checkLastModified(lastModified)) {
                System.out.println("Not modified yet.");
            } else {
                insertCurrencies(url);
                setLastModified(lastModified);
            }
        }
    }

    private static NodeList getCurrencies(URL url) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//an instance of builder to parse the specified xml file
        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
            Document doc = db.parse(url.openStream());
            doc.getDocumentElement().normalize();

            System.out.println("Root element: " + doc.getDocumentElement().getNodeName());

            return doc.getElementsByTagName("Currency");

        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private static void insertCurrencies(URL url) {
        NodeList nodeList = getCurrencies(url);
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("persistence-unit");
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction entityTransaction = entityManager.getTransaction();

        for (int itr = 0; itr < nodeList.getLength(); itr++) {
            Node node = nodeList.item(itr);
            System.out.println("\nNode Name :" + node.getNodeName());
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) node;
                entityTransaction.begin();
                String currencyCode = eElement.getAttribute("CurrencyCode");
                int unit = Integer.parseInt(eElement.getElementsByTagName("Unit").item(0).getTextContent());
                double forexBuying = Double.parseDouble(eElement.getElementsByTagName("ForexBuying").item(0).getTextContent());

                if (eElement.getAttribute("CurrencyCode").equals("XDR")) {
                    double crossRateOther = Double.parseDouble(eElement.getElementsByTagName("CrossRateOther").item(0).getTextContent());
                    Information information = new Information(itr, new Date(), currencyCode, unit, crossRateOther, forexBuying);
                    entityManager.persist(information);
                    System.out.println(information);
                } else {
                    double forexSelling = Double.parseDouble(eElement.getElementsByTagName("ForexSelling").item(0).getTextContent());
                    Forex forex = new Forex(itr, new Date(), currencyCode, unit, forexBuying, forexSelling);
                    entityManager.persist(forex);
                    System.out.println(forex);

                    if (!eElement.getElementsByTagName("CrossRateUSD").item(0).getTextContent().equals("") ) {
                        double crossRateOther = Double.parseDouble(eElement.getElementsByTagName("CrossRateUSD").item(0).getTextContent());

                        CrossRates crossRates = new CrossRates(itr, new Date(), currencyCode, unit, crossRateOther);
                        entityManager.persist(crossRates);
                        System.out.println(crossRates);

                    } else if(!eElement.getElementsByTagName("CrossRateOther").item(0).getTextContent().equals("")){
                        double crossRateUSD = Double.parseDouble(eElement.getElementsByTagName("CrossRateOther").item(0).getTextContent());
                        CrossRates crossRates = new CrossRates(itr, new Date(), currencyCode, unit, crossRateUSD);
                        entityManager.persist(crossRates);
                        System.out.println(crossRates);
                    }

                    if (eElement.getElementsByTagName("BanknoteBuying").item(0).getTextContent().equals("") || eElement.getElementsByTagName("BanknoteSelling").item(0).getTextContent().equals("")) {
                        Banknote banknote = new Banknote(itr, new Date(), currencyCode, unit);
                        entityManager.persist(banknote);
                        System.out.println(banknote);

                    } else {
                        double banknoteBuying = Double.parseDouble(eElement.getElementsByTagName("BanknoteBuying").item(0).getTextContent());
                        double banknoteSelling = Double.parseDouble(eElement.getElementsByTagName("BanknoteSelling").item(0).getTextContent());
                        Banknote banknote = new Banknote(itr, new Date(), currencyCode, unit, banknoteBuying, banknoteSelling);
                        entityManager.persist(banknote);
                        System.out.println(banknote);
                    }
                }
                entityTransaction.commit();
            }
        }
    }

    private static void setLastModified(long lastModified) {
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

    private static boolean checkLastModified(long lastModified) throws IOException {
        PropertiesConfiguration config = null;
        try {
            config = new PropertiesConfiguration("config.properties");
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
        String configString = config.getString("last.modified");
        return configString.equals(String.valueOf(lastModified));
    }

}
