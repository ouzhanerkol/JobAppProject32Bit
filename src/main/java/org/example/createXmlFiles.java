package org.example;

import org.example.entities.Forex;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

public class createXmlFiles {

    Element forex;
    public static void main(String argv[]) {
        try {
            DocumentBuilderFactory dbFactory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            // root element
            Element rootElement = doc.createElement("Data");
            Attr attrType = doc.createAttribute("Date");
            attrType.setValue("02/09/2024"); //TODO: date
            doc.appendChild(rootElement);
            rootElement.setAttributeNode(attrType);

            // supercars element
            Element forex = doc.createElement("Forex");
            rootElement.appendChild(forex);
            Element banknote = doc.createElement("Banknote");
            rootElement.appendChild(banknote);
            Element cross = doc.createElement("Cross");
            rootElement.appendChild(cross);
            Element information = doc.createElement("Information");
            rootElement.appendChild(information);
            for (int i = 0; i < 5; i++) {
                Element currency = doc.createElement("Currency");
                Attr attrPair = doc.createAttribute("Pair");
                attrPair.setValue("USD/TRY"); //TODO: currencyCode
                currency.setAttributeNode(attrPair);
                Attr attrUnit = doc.createAttribute("Unit");
                attrUnit.setValue("1"); // TODO: unit
                currency.setAttributeNode(attrUnit);
                Attr attrBuy = doc.createAttribute("Buy");
                attrBuy.setValue("30"); // TODO: forexBuying
                currency.setAttributeNode(attrBuy);
                Attr attrSell = doc.createAttribute("Sell");
                attrSell.setValue("30"); // TODO: forexSelling
                currency.setAttributeNode(attrSell);
                forex.appendChild(currency);
            }

            for (int i = 0; i < 5; i++) {
                Element currency = doc.createElement("Currency");
                Attr attrPair = doc.createAttribute("Pair");
                attrPair.setValue("USD/TRY"); //TODO: currencyCode
                currency.setAttributeNode(attrPair);
                Attr attrUnit = doc.createAttribute("Unit");
                attrUnit.setValue("1"); // TODO: unit
                currency.setAttributeNode(attrUnit);
                Attr attrBuy = doc.createAttribute("Buy");
                attrBuy.setValue("30"); // TODO: forexBuying
                currency.setAttributeNode(attrBuy);
                Attr attrSell = doc.createAttribute("Sell");
                attrSell.setValue("30"); // TODO: forexSelling
                currency.setAttributeNode(attrSell);

                banknote.appendChild(currency);
            }

            for (int i = 0; i < 5; i++) {
                Element currency = doc.createElement("Currency");
                Attr attrPair = doc.createAttribute("Pair");
                attrPair.setValue("USD/TRY"); //TODO: currencyCode
                currency.setAttributeNode(attrPair);
                Attr attrUnit = doc.createAttribute("Unit");
                attrUnit.setValue("1"); // TODO: unit
                currency.setAttributeNode(attrUnit);
                Attr attrBuy = doc.createAttribute("Rate");
                attrBuy.setValue("1.5"); // TODO: forexBuying
                currency.setAttributeNode(attrBuy);

                cross.appendChild(currency);
            }

            // carname element
            Element currency = doc.createElement("Currency");
            Attr attrType1 = doc.createAttribute("Pair");
            attrType1.setValue("USD/TRY");
            currency.setAttributeNode(attrType1);
            forex.appendChild(currency);

            Element currency1 = doc.createElement("Currency");
            Attr attrType2 = doc.createAttribute("type");
            attrType2.setValue("sports");
            currency1.setAttributeNode(attrType2);
            forex.appendChild(currency1);

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("Rates_"+"20240220" + "_" + "123001" + ".xml"));
            transformer.transform(source, result);

            // Output to console for testing
            StreamResult consoleResult = new StreamResult(System.out);
            transformer.transform(source, consoleResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setForexAttributes(Document doc, Forex forex){
        Element currency = doc.createElement("Currency");
        Attr attrPair = doc.createAttribute("Pair");
        attrPair.setValue(forex.getCurrencyCode() + "/TRY");
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

        //return forex.appendChild(currency);
    }
}
