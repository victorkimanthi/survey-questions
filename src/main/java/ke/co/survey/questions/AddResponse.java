package ke.co.survey.questions;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.HeaderValues;
import ke.co.skyworld.ancillaries.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.ancillaries.query_manager.beans.TransactionWrapper;
import ke.co.skyworld.ancillaries.query_repository.Repository;
import ke.co.skyworld.ancillaries.restful_undertow.authentication_handlers.ExchangeContextAuthenticated;
import ke.co.skyworld.ancillaries.restful_undertow.beans.UserSessionContext;
import ke.co.skyworld.ancillaries.restful_undertow.utils.ExchangeResponse;
import ke.co.skyworld.ancillaries.restful_undertow.utils.ExchangeUtils;
import ke.co.skyworld.ancillaries.utility_items.Misc;
import ke.co.skyworld.ancillaries.utility_items.constants.StringRefs;
import ke.co.survey.UTILS.XmlReader;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddResponse extends ExchangeContextAuthenticated {
    @Override
    public void handleRequest(HttpServerExchange httpServerExchange, UserSessionContext userSessionContext, Object... objects) {

        TransactionWrapper wrapper = new TransactionWrapper<>();
        String organizationId = StringRefs.SENTINEL;
//        String fullTempFileName = null;
        StringBuilder filesStringBuilder = new StringBuilder();
        List<String> filesList = new ArrayList<>();
        try {
            //get authorization
            HeaderValues authHeader = httpServerExchange.getRequestHeaders().get("Authorization");

            String authHeaderString = authHeader.get(0);
            //getting formData
            FormData formData2 = httpServerExchange.getAttachment(FormDataParser.FORM_DATA);
//            System.out.println("response details:"+formData2.get("response_details").getFirst().getValue());

            //getting organization details
//            flexicoreHashMap = ExchangeUtils.getBodyObjectWithGson(httpServerExchange, FlexicoreHashMap.class, formData2.get("response_details").getFirst().getValue());
//            String xmlString = ExchangeUtils.getBodyObjectWithGson(httpServerExchange, String.class, formData2.get("response_details").getFirst().getValue());
            String xmlString = ExchangeUtils.getFormData(httpServerExchange, "response_details");
//            System.out.println("flexicoreHashMap:" + flexicoreHashMap);
            System.out.println("xmlString:" + xmlString);

//            if (flexicoreHashMap == null || flexicoreHashMap.isEmpty()) {
            if (xmlString == null || xmlString.isEmpty()) {
                ExchangeResponse.sendBadRequest(httpServerExchange, "You have not provided any data on the required fields.");
                return;
            }

            FlexicoreHashMap responseMap = parseXMLToMap(xmlString);
            if (responseMap != null) {
                responseMap.put("interviewee_id", authHeaderString);
            }

            System.out.println("responseMap:" + responseMap);

            if (formData2.contains("certificates") && formData2.get("certificates") != null) {
                // Base directory where user directories will be created
                String baseDirectory =XmlReader.getFileUploadPath();

                System.out.println("baseDirectory:"+baseDirectory);

                // Create a directory path based on the user ID
                String userDirectoryPath = baseDirectory + authHeaderString;
                System.out.println("userDirectoryPath:"+userDirectoryPath);

                // Create the user directory if it doesn't exist
                File userDirectory = new File(userDirectoryPath);
                if (!userDirectory.exists()) {
                    boolean created = userDirectory.mkdir();
                    if (created) {
                        System.out.println("Created directory for user: " + authHeaderString);
                    } else {
                        System.err.println("Failed to create directory for user: " + authHeaderString);
                    }
                } else {
                    System.out.println("User directory already exists for user: " + authHeaderString);
                }

                Deque<FormData.FormValue> deque = formData2.get("certificates");
                for (FormData.FormValue formValue : deque) {
                    //checking if file is empty
             /*   if (formValue.getFileItem().getFileSize() == 0) {
                    ExchangeResponse.sendBadRequest(httpServerExchange, "A required file is missing.Check to ensure that you have entered all the required fields.");
                    return;
                }*/

                    File tempFile = formValue.getFileItem().getFile().toFile();
                    String realFileName = formValue.getFileName();
//                    String fileExtension = getFileExtension(realFileName);
//                    String tempFileName = tempFile.getName();
//                    fullTempFileName = tempFileName + "." + fileExtension;

                    //checking the size of the files/file
                    long fileSizeInBytes = tempFile.length();
                    long fileSizeInMB = fileSizeInBytes / (1024 * 1024);
                    long fileSizeInMBDecimals = fileSizeInBytes % (1024 * 1024);
                    System.out.println("fileSizeInMB:" + fileSizeInMB);
                    System.out.println("fileSizeInMBDecimals:" + fileSizeInMBDecimals);

                    if (fileSizeInMB >= 5 && fileSizeInMBDecimals > 0) {
                        ExchangeResponse.sendBadRequest(httpServerExchange, "File too big.The size of the file is greater than 5mb.");
                        return;
                    }

                    //getting the upload folder path from the xml file**********************************

//                Files.move(Paths.get(tempFile.getPath()), Paths.get(uploadFolderPath + fullTempFileName), StandardCopyOption.REPLACE_EXISTING);
                    Files.move(Paths.get(tempFile.getPath()), Paths.get(userDirectoryPath + "/" + realFileName), StandardCopyOption.REPLACE_EXISTING);
                    filesList.add(realFileName);
                }

                System.out.println("filesList:" + filesList);
                int noOfFiles = filesList.size();
                System.out.println("no of files" + noOfFiles);
                for (int i = 0; i < noOfFiles; i++) {
                    if (noOfFiles == 1 || (i == noOfFiles - 1)) {
                        filesStringBuilder.append(filesList.get(i));
                    } else {
                        System.out.println("list.get(i):" + filesList.get(i));
                        filesStringBuilder.append(filesList.get(i));
                        filesStringBuilder.append(",");
                    }
                }

                if (responseMap != null) {
                    responseMap.put("certificates", filesStringBuilder.toString());
                }
            }

            //query to insert into organization details table
            if (responseMap != null) {
                wrapper = Repository.insertAutoIncremented(organizationId, "responses", responseMap);
            }
            System.out.println("responseMap:" + responseMap);

            if (wrapper.hasErrors()) {
                if (wrapper.getErrors().contains("duplicate key value")) {
                    ExchangeResponse.sendForbidden(httpServerExchange,"The field "+ extractString(wrapper.getErrors())+"Confirm to ensure you entered the right value.");
                } else {
                    ExchangeResponse.sendInternalServerError(httpServerExchange, "An error occurred while trying to create that response.");
                }
                return;
            }
            ExchangeResponse.sendOK(httpServerExchange, "Response  has been  uploaded successfully.");

        } catch (Exception e) {
            ExchangeResponse.sendInternalServerError(httpServerExchange, Misc.getTransactionWrapperStackTrace(e));
        }
    }

    public static FlexicoreHashMap parseXMLToMap(String xmlString) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlString)));

//            Map<String, String> resultMap = new LinkedHashMap<>();
            FlexicoreHashMap resultMap = new FlexicoreHashMap();

            NodeList questions = document.getElementsByTagName("question");
            for (int i = 0; i < questions.getLength(); i++) {
                Element question = (Element) questions.item(i);
                String name = question.getAttribute("name");
                String text = question.getTextContent().trim();

                // Handle special case for "frontend_stack" questions
                if ("frontend_stack".equals(name)) {
                    StringBuilder selectedValues = getSelectedValues(question);
                    resultMap.put(name, selectedValues.toString());
                } else {
                    resultMap.put(name, text);
                }
            }

            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @NotNull
    private static StringBuilder getSelectedValues(Element question) {
        NodeList selectedOptions = question.getElementsByTagName("selected_option");
        StringBuilder selectedValues = new StringBuilder();
        for (int j = 0; j < selectedOptions.getLength(); j++) {
            Element selectedOption = (Element) selectedOptions.item(j);
            selectedValues.append(selectedOption.getTextContent().trim());
            if (j < selectedOptions.getLength() - 1) {
                selectedValues.append(", ");
            }
        }
        return selectedValues;
    }

    String extractString(String string) {
//            String stringInput = "ERROR: duplicate key value violates unique constraint \"vendor_user_groups_group_name_uindex\"\n  Detail: Key (group_name)=(Backend Developers) already exists.";

        String stringInput = string;
        String detailPart = "";

        // Define the pattern using regular expression
        Pattern pattern = Pattern.compile("Detail: Key (.*)");

        // Create a matcher using the pattern and input string
        Matcher matcher = pattern.matcher(stringInput);

        // Find the match and extract the captured group
        if (matcher.find()) {
            detailPart = matcher.group(1).trim();
//                System.out.println("Detail part: " + detailPart);
        } else {
            System.out.println("Detail part not found.");
        }
        return  detailPart;
    }
}

