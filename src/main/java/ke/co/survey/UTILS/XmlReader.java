package ke.co.survey.UTILS;

import ke.co.skyworld.ancillaries.query_manager.beans.FlexicoreHashMap;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;

public class XmlReader {

       /* {
            try {
                File inputFile = new File("config.xml");
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder;

                dBuilder = dbFactory.newDocumentBuilder();

                Document doc = dBuilder.parse(inputFile);
                doc.getDocumentElement().normalize();

                XPath xPath = XPathFactory.newInstance().newXPath();
            } catch (ParserConfigurationException | SAXException | IOException e) {
                throw new RuntimeException(e);
            }*/

            public static String getFileUploadPath(){
            String filesUploadPath = null;
            try {
                File inputFile = new File("config.xml");
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder;

                dBuilder = dbFactory.newDocumentBuilder();

                Document doc = dBuilder.parse(inputFile);
                doc.getDocumentElement().normalize();

                XPath xPath = XPathFactory.newInstance().newXPath();

                String expression = "/API/FILES/FILESUPLOADPATH";

                filesUploadPath  = (String) xPath.compile(expression).evaluate(doc, XPathConstants.STRING);

            }  catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
                e.printStackTrace();
            }
            return filesUploadPath;
        }

        public static LinkedHashMap <String,Object> getAccessTokenValidityDetails(String xmlString) {
//            String xml = xmlString;
           /* String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<access_token_configuration>" +
                    "<validity_time>24</validity_time>" +
                    "<validity_time_units>hours</validity_time_units>" +
                    "</access_token_configuration>";*/

            try {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(new InputSource(new StringReader(xmlString)));

                // Create an XPath instance
                XPathFactory xPathFactory = XPathFactory.newInstance();
                XPath xPath = xPathFactory.newXPath();

                // Define XPath expressions for the elements
                XPathExpression validityTimeExpr = xPath.compile("/access_token_configuration/validity_time/text()");
                XPathExpression validityTimeUnitsExpr = xPath.compile("/access_token_configuration/validity_time_units/text()");

                // Evaluate the XPath expressions to get the values
                double validityTime = (double) validityTimeExpr.evaluate(doc, XPathConstants.NUMBER);
                String validityTimeUnits = (String) validityTimeUnitsExpr.evaluate(doc, XPathConstants.STRING);

                System.out.println("Validity Time: " + validityTime);
                System.out.println("Validity Time Units: " + validityTimeUnits);

                FlexicoreHashMap flexicoreHashMap = new FlexicoreHashMap();
                flexicoreHashMap.put("access_token_validity_time",validityTime);
                flexicoreHashMap.put("access_token_validity_time_units",validityTimeUnits);

                return flexicoreHashMap;
            } catch (Exception e) {
                e.printStackTrace();
                return new LinkedHashMap<>();
            }
        }

        public static LinkedHashMap <String,Object> getGeneratedPasswordValidityDetails(String xmlString) {
//            String xml = xmlString;
           /* String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<access_token_configuration>" +
                    "<validity_time>24</validity_time>" +
                    "<validity_time_units>hours</validity_time_units>" +
                    "</access_token_configuration>";*/

            try {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(new InputSource(new StringReader(xmlString)));

                // Create an XPath instance
                XPathFactory xPathFactory = XPathFactory.newInstance();
                XPath xPath = xPathFactory.newXPath();

                // Define XPath expressions for the elements
                XPathExpression validityTimeExpr = xPath.compile("/generated_password_configuration/validity_time/text()");
                XPathExpression validityTimeUnitsExpr = xPath.compile("/generated_password_configuration/validity_time_units/text()");

                // Evaluate the XPath expressions to get the values
                double validityTime = (double) validityTimeExpr.evaluate(doc, XPathConstants.NUMBER);
                String validityTimeUnits = (String) validityTimeUnitsExpr.evaluate(doc, XPathConstants.STRING);

                System.out.println("Validity Time: " + validityTime);
                System.out.println("Validity Time Units: " + validityTimeUnits);

                FlexicoreHashMap flexicoreHashMap = new FlexicoreHashMap();
                flexicoreHashMap.put("generated_password_validity_time",validityTime);
                flexicoreHashMap.put("generated_password_validity_time_units",validityTimeUnits);

                return flexicoreHashMap;
            } catch (Exception e) {
                e.printStackTrace();
                return new LinkedHashMap<>();
            }
        }
    }

