package org.example;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;

public class XSLTProcessor {

    public static void main(String[] args) {
        try {
            transformXMLUsingXSLT("input.xml", "stylesheet.xslt", "output.xml");
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void transformXMLUsingXSLT(String inputXMLPath, String xsltPath, String outputXMLPath) throws TransformerException {
        Source xmlSource = new StreamSource(new File(inputXMLPath));
        Source xsltSource = new StreamSource(new File(xsltPath));
        Result output = new StreamResult(new File(outputXMLPath));

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer(xsltSource);
        transformer.transform(xmlSource, output);
    }
}
