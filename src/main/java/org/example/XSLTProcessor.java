package org.example;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;

public class XSLTProcessor {
    /**
     * this method transforms data from one xml file
     * to another xml file using xslt file
     *
     * @param inputXMLPath - path of the xml file to be processed
     * @param xsltPath - path of the xslt file to be used
     * @param outputXMLPath - path of the new xml file
     * @throws TransformerException
     */
    public static void transformXMLUsingXSLT(String inputXMLPath, String xsltPath, String outputXMLPath) throws TransformerException {
        //set source files
        Source xmlSource = new StreamSource(new File(inputXMLPath));
        Source xsltSource = new StreamSource(new File(xsltPath));
        Result output = new StreamResult(new File(outputXMLPath));

        //Create transformer Factory and transformer from xslt source
        // and transform xml to new xml with transformer
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer(xsltSource);
        transformer.transform(xmlSource, output);
    }
}
