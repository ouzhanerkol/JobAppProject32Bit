package org.example;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.example.model.Banknote;
import org.example.model.Forex;
import org.example.model.Information;
import org.xml.sax.SAXException;

import javax.net.ssl.HttpsURLConnection;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.io.*;
import java.net.URL;
import java.util.Date;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.io.File;

public class Main {
    private static final String NAME_OF_URL = "https://www.tcmb.gov.tr/kurlar/today.xml";
    public static void main(String[] args) throws IOException {
        URL url = new URL("https://www.tcmb.gov.tr/kurlar/today.xml");
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.connect();
        int connectionResponseCode = urlConnection.getResponseCode();
        long lastModified = urlConnection.getLastModified();
        String fileModifiedDate = null;

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


        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("persistence-unit");
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction entityTransaction = entityManager.getTransaction();

        //System.out.println("Response Code: " + connectionResponseCode);
        String configString = config.getString("last.modified");
        if (configString.equals(String.valueOf(lastModified))) {

            System.out.println("Last Modified: " + new Date(lastModified));

            // JAXB Kullanarak XML İşleme
            try {
//creating a constructor of file class and parsing an XML file
                File file = new File("F:\\XMLFile.xml");
//an instance of factory that gives a document builder
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//an instance of builder to parse the specified xml file
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(url.openStream());
                doc.getDocumentElement().normalize();
                System.out.println("Root element: " + doc.getDocumentElement().getNodeName());
                NodeList nodeList = doc.getElementsByTagName("Currency");
// nodeList is not iterable, so we are using for loop
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
            } catch (ParserConfigurationException | SAXException e) {
                throw new RuntimeException(e);
            }

        } else {
            System.out.println("Error in sending a GET request: " + connectionResponseCode);
        }
    }
}