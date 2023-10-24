package ke.co.survey.responses;

import io.undertow.server.HttpServerExchange;
import ke.co.skyworld.ancillaries.query_manager.beans.FlexicoreArrayList;
import ke.co.skyworld.ancillaries.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.ancillaries.query_manager.beans.TransactionWrapper;
import ke.co.skyworld.ancillaries.query_manager.query.FilterPredicate;
import ke.co.skyworld.ancillaries.query_repository.Repository;
import ke.co.skyworld.ancillaries.restful_undertow.authentication_handlers.ExchangeContextAuthenticated;
import ke.co.skyworld.ancillaries.restful_undertow.beans.UserSessionContext;
import ke.co.skyworld.ancillaries.restful_undertow.utils.ExchangeResponse;
import ke.co.skyworld.ancillaries.utility_items.Misc;
import ke.co.skyworld.ancillaries.utility_items.constants.StringRefs;


public class GetIndividualResponse extends ExchangeContextAuthenticated {
    @Override
    public void handleRequest(HttpServerExchange httpServerExchange, UserSessionContext userSessionContext, Object... objects) {

        try {

            String intervieweeId2 = httpServerExchange.getRequestHeaders().get("Authorization").get(0);

            TransactionWrapper<FlexicoreHashMap> wrapper = Repository.selectWhere(StringRefs.SENTINEL, "responses", "DISTINCT full_name,gender,email_address,description,frontend_stack,date_created as date_responded",
                    new FilterPredicate("interviewee_id = :interviewee_id"),
                    new FlexicoreHashMap().addQueryArgument(":interviewee_id", intervieweeId2));

            if (wrapper.hasErrors()) {
                ExchangeResponse.sendInternalServerError(httpServerExchange, wrapper.getErrors());
                return;
            }

            FlexicoreHashMap flexicoreHashMap = wrapper.getSingleRecord();
            FlexicoreArrayList flexicoreArrayList;
//            String certificatesString = flexicoreHashMap.getStringValue("certificates");

            //getting files of interviewee
            TransactionWrapper<FlexicoreArrayList> wrapper2 = Repository.selectWhere(StringRefs.SENTINEL, "response_attachments", "DISTINCT file_id,file_name",
                    new FilterPredicate("interviewee_id = :interviewee_id"),
                    new FlexicoreHashMap().addQueryArgument(":interviewee_id", intervieweeId2));

            if (wrapper2.hasErrors()) {
                ExchangeResponse.sendInternalServerError(httpServerExchange, wrapper2.getErrors());
                return;
            }

           /* if (certificatesString.contains(",")) {
                String[] stringArray = certificatesString.split(",");

                for (String fileName : stringArray) {
                    FlexicoreHashMap flexicoreHashMap2 = new FlexicoreHashMap();
                    File file = new File(XmlReader.getFileUploadPath() + intervieweeId2 + File.separator + fileName);
                    byte[] fileContent = FileUtils.readFileToByteArray(file);
                    encodedString = Base64.getEncoder().encodeToString(fileContent);
                    flexicoreHashMap2.put("file_name", fileName);
                    flexicoreHashMap2.put("file_content", encodedString);
                    flexicoreArrayList.add(flexicoreHashMap2);
                }

            } else {
                File file = new File(XmlReader.getFileUploadPath() + intervieweeId2 + File.separator + certificatesString);
                byte[] fileContent = FileUtils.readFileToByteArray(file);
                encodedString = Base64.getEncoder().encodeToString(fileContent);
                FlexicoreHashMap flexicoreHashMap2 = new FlexicoreHashMap();
                flexicoreHashMap2.put("file_name", certificatesString);
                flexicoreHashMap2.put("file_content", encodedString);
                flexicoreArrayList.add(flexicoreHashMap2);
            }*/

            flexicoreArrayList = wrapper2.getData();
            if(flexicoreArrayList != null && !flexicoreArrayList.isEmpty()){
                for (FlexicoreHashMap flexicoreHashMap1:flexicoreArrayList) {
                    flexicoreHashMap1.put("fileId",flexicoreHashMap1.get("file_id"));
                    flexicoreHashMap1.put("fileName",flexicoreHashMap1.get("file_name"));
                    flexicoreHashMap1.remove("file_id");
                    flexicoreHashMap1.remove("file_name");
                }
            }
            flexicoreHashMap.put("certificates", flexicoreArrayList);

            //sending  response that records have been fetched successfully
            ExchangeResponse.sendOK(httpServerExchange, flexicoreHashMap);
        } catch (Exception e) {
            ExchangeResponse.sendInternalServerError(httpServerExchange, Misc.getTransactionWrapperStackTrace(e));
        }
    }

}
