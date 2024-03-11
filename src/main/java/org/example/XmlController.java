package org.example;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.example.model.Banknote;
import org.example.model.CrossRates;
import org.example.model.Forex;
import org.example.model.Information;
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
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class XmlController {

    private static final String NAME_OF_URL = "https://www.tcmb.gov.tr/kurlar/today.xml";

    private static final Logger LOG = LogManager.getLogger(XmlController.class);

    private Document doc;

    public void connectURL() throws IOException {
        URL url = new URL(NAME_OF_URL);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.connect();

        int connectionResponseCode = urlConnection.getResponseCode();
        long lastModified = urlConnection.getLastModified();

        if (connectionResponseCode == HttpsURLConnection.HTTP_OK) {
            if (getLastModified().equals(String.valueOf(lastModified))) {
                LOG.info("Not modified yet...");
            } else {
                insertCurrencies(url);
                setLastModified(lastModified);
            }
        }
    }

    public NodeList getCurrencies(URL url) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//an instance of builder to parse the specified xml file
        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
            Document doc = db.parse(url.openStream());
            doc.getDocumentElement().normalize();

            System.out.println("Root element: " + doc.getDocumentElement().getNodeName());
            LOG.info("getting Currencies successful");
            return doc.getElementsByTagName("Currency");

        } catch (ParserConfigurationException | IOException | SAXException e) {
            System.out.print(e.getMessage());
            LOG.error("Exception occured", new Exception("Document building failed.."));
            return null;
        }
    }

    public void insertCurrencies(URL url) {
        NodeList nodeList = getCurrencies(url);
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("persistence-unit");
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction entityTransaction = entityManager.getTransaction();
        List<Element> elements;
        elements = createXmlElements();

        for (int itr = 0; itr < nodeList.getLength(); itr++) {
            Node node = nodeList.item(itr);
            LOG.info("Collecting tags...");
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) node;
                entityTransaction.begin();

                String currencyCode = eElement.getAttribute("CurrencyCode");
                int unit = Integer.parseInt(eElement.getElementsByTagName("Unit").item(0).getTextContent());
                double forexBuying = Double.parseDouble(eElement.getElementsByTagName("ForexBuying").item(0).getTextContent());

                if (eElement.getAttribute("CurrencyCode").equals("XDR")) {
                    double crossRateOther = Double.parseDouble(eElement.getElementsByTagName("CrossRateOther").item(0).getTextContent());
                    Information information = new Information(new Date(), currencyCode, unit, crossRateOther, forexBuying);
                    createXmlAttributeInformation(elements, information);
                    entityManager.persist(information);
                    LOG.info("Information finished");
                } else {
                    double forexSelling = Double.parseDouble(eElement.getElementsByTagName("ForexSelling").item(0).getTextContent());
                    Forex forex = new Forex(new Date(), currencyCode, unit, forexBuying, forexSelling);
                    createXmlAttributeForex(elements, forex);
                    entityManager.persist(forex);


                    if (!eElement.getElementsByTagName("CrossRateUSD").item(0).getTextContent().equals("")) {
                        double crossRateOther = Double.parseDouble(eElement.getElementsByTagName("CrossRateUSD").item(0).getTextContent());

                        CrossRates crossRates = new CrossRates(new Date(), currencyCode, unit, crossRateOther);
                        createXmlAttributeCross(elements, crossRates);
                        entityManager.persist(crossRates);

                    } else if (!eElement.getElementsByTagName("CrossRateOther").item(0).getTextContent().equals("")) {
                        double crossRateUSD = Double.parseDouble(eElement.getElementsByTagName("CrossRateOther").item(0).getTextContent());
                        CrossRates crossRates = new CrossRates(new Date(), currencyCode, unit, crossRateUSD);
                        entityManager.persist(crossRates);
                    }

                    if (eElement.getElementsByTagName("BanknoteBuying").item(0).getTextContent().equals("") || eElement.getElementsByTagName("BanknoteSelling").item(0).getTextContent().equals("")) {
                        Banknote banknote = new Banknote(new Date(), currencyCode, unit);
                        createXmlAttributeBanknote(elements, banknote);
                        entityManager.persist(banknote);

                    } else {
                        double banknoteBuying = Double.parseDouble(eElement.getElementsByTagName("BanknoteBuying").item(0).getTextContent());
                        double banknoteSelling = Double.parseDouble(eElement.getElementsByTagName("BanknoteSelling").item(0).getTextContent());
                        Banknote banknote = new Banknote(new Date(), currencyCode, unit, banknoteBuying, banknoteSelling);
                        createXmlAttributeBanknote(elements, banknote);
                        entityManager.persist(banknote);
                    }
                }
                entityTransaction.commit();
                LOG.info("Currencies added successfully");
            }
        }

        createXmlFile();
    }

    private void createXmlFile() {
        LOG.info("Xml file creating");
        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;

        try {
            transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("Rates_" + getYyyyMMdd() + "_" + getHHmmss() + ".xml"));
            transformer.transform(source, result);
        } catch (TransformerException e) {
            System.out.print(e.getMessage());
            LOG.error("Exception occured", new Exception("Creating xml failed.."));
        }
    }

    private String getYyyyMMdd() {
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
        return f.format(new Date());
    }
    private String getHHmmss() {
        SimpleDateFormat f = new SimpleDateFormat("HHmmss");
        return f.format(new Date());
    }

    private void createXmlAttributeForex(List<Element> elements, Forex forex){
        Element currency = doc.createElement("Currency");
        Attr attrPair = doc.createAttribute("Pair");
        attrPair.setValue(forex.getCurrencyCode() + "/USD");
        currency.setAttributeNode(attrPair);

        Attr attrUnit = doc.createAttribute("Unit");
        attrUnit.setValue(String.valueOf(forex.getUnit()));
        currency.setAttributeNode(attrUnit);

        Attr attrBuy = doc.createAttribute("Buy");
        attrBuy.setValue(String.valueOf(forex.getForexBuying()));
        currency.setAttributeNode(attrBuy);

        Attr attrSell = doc.createAttribute("Sell");
        attrSell.setValue(String.valueOf(forex.getForexSelling()));
        currency.setAttributeNode(attrSell);

        elements.get(0).appendChild(currency);
        LOG.info("Exception occured");
    }

    private void createXmlAttributeBanknote(List<Element> elements, Banknote banknote) {
        Element currency = doc.createElement("Currency");
        Attr attrPair = doc.createAttribute("Pair");
        attrPair.setValue(banknote.getCurrencyCode() + "/TRY");
        currency.setAttributeNode(attrPair);

        Attr attrUnit = doc.createAttribute("Unit");
        attrUnit.setValue(String.valueOf(banknote.getUnit()));
        currency.setAttributeNode(attrUnit);

        Attr attrBuy = doc.createAttribute("Buy");
        attrBuy.setValue(String.valueOf(banknote.getBanknoteBuying()));
        currency.setAttributeNode(attrBuy);

        Attr attrSell = doc.createAttribute("Sell");
        attrSell.setValue(String.valueOf(banknote.getBanknoteSelling()));
        currency.setAttributeNode(attrSell);

        elements.get(1).appendChild(currency);
    }

    private void createXmlAttributeCross(List<Element> elements, CrossRates crossRates) {
        Element currency = doc.createElement("Currency");
        Attr attrPair = doc.createAttribute("Pair");
        attrPair.setValue("USD/" + crossRates.getCurrencyCode());
        currency.setAttributeNode(attrPair);

        Attr attrUnit = doc.createAttribute("Unit");
        attrUnit.setValue(String.valueOf(crossRates.getUnit()));
        currency.setAttributeNode(attrUnit);

        Attr attrRate = doc.createAttribute("Rate");
        attrRate.setValue(String.valueOf(crossRates.getCrossRate()));
        currency.setAttributeNode(attrRate);

        elements.get(2).appendChild(currency);
    }

    private void createXmlAttributeInformation(List<Element> elements, Information information) {
        Element currencyUsd = doc.createElement("Currency");
        Attr attrPair = doc.createAttribute("Pair");
        attrPair.setValue("SDR/USD");
        currencyUsd.setAttributeNode(attrPair);

        Attr attrUnit = doc.createAttribute("Unit");
        attrUnit.setValue(String.valueOf(information.getUnit()));
        currencyUsd.setAttributeNode(attrUnit);

        Attr attrBuy = doc.createAttribute("Rate");
        attrBuy.setValue(String.valueOf(information.getInformationUSD()));
        currencyUsd.setAttributeNode(attrBuy);

        Element currencyTry = doc.createElement("Currency");
        Attr attrPair1 = doc.createAttribute("Pair");
        attrPair1.setValue("SDR/TRY");
        currencyTry.setAttributeNode(attrPair1);

        Attr attrUnit1 = doc.createAttribute("Unit");
        attrUnit1.setValue(String.valueOf(information.getUnit()));
        currencyTry.setAttributeNode(attrUnit1);

        Attr attrBuy1 = doc.createAttribute("Rate");
        attrBuy1.setValue(String.valueOf(information.getInformationTRY()));
        currencyTry.setAttributeNode(attrBuy1);

        elements.get(3).appendChild(currencyUsd);
        elements.get(3).appendChild(currencyTry);
    }

    public void setDoc() {
        DocumentBuilderFactory dbFactory =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        this.doc = dBuilder.newDocument();
    }

    private List<Element> createXmlElements() {
        setDoc();
        List<Element> elements = new ArrayList<>();
        // root element
        Element rootElement = doc.createElement("Data");
        Attr attrType = doc.createAttribute("Date");
        attrType.setValue("02/09/2024"); //TODO: date
        doc.appendChild(rootElement);
        rootElement.setAttributeNode(attrType);

        // child elements
        Element forex = doc.createElement("Forex");
        rootElement.appendChild(forex);
        elements.add(forex);
        Element banknote = doc.createElement("Banknote");
        rootElement.appendChild(banknote);
        elements.add(banknote);
        Element cross = doc.createElement("Cross");
        rootElement.appendChild(cross);
        elements.add(cross);
        Element information = doc.createElement("Information");
        rootElement.appendChild(information);
        elements.add(information);
        return elements;
    }

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
