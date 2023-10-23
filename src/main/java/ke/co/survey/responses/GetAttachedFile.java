package ke.co.survey.responses;

import io.undertow.server.HttpServerExchange;
import ke.co.skyworld.ancillaries.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.ancillaries.query_manager.beans.TransactionWrapper;
import ke.co.skyworld.ancillaries.query_manager.query.FilterPredicate;
import ke.co.skyworld.ancillaries.query_repository.Repository;
import ke.co.skyworld.ancillaries.restful_undertow.interfaces.ExchangeHandler;
import ke.co.skyworld.ancillaries.restful_undertow.utils.ExchangeResponse;
import ke.co.skyworld.ancillaries.restful_undertow.utils.ExchangeUtils;
import ke.co.skyworld.ancillaries.utility_items.Misc;
import ke.co.skyworld.ancillaries.utility_items.constants.StringRefs;
import ke.co.survey.UTILS.XmlReader;

import java.io.File;
import java.util.HashMap;

public class GetAttachedFile implements ExchangeHandler {
    @Override
    public void handleRequest(HttpServerExchange httpServerExchange, Object... objects) throws Exception {

        String  organizationId = StringRefs.SENTINEL;

        String [] parameterKeys= {"fileId"};

        try {

            HashMap<String,String> parameters = ExchangeUtils.getQueryParams(httpServerExchange,parameterKeys);
            System.out.println("parameters:"+ parameters);

            //select all applications query order by date created with pagination /without pagination

           TransactionWrapper<FlexicoreHashMap> wrapper = Repository.selectWhere(organizationId, "response_attachments", "DISTINCT file_name,interviewee_id",
                    new FilterPredicate("file_id = :file_id"),
                    new FlexicoreHashMap().addQueryArgument(":file_id", parameters.get("fileId")));

            if (wrapper.hasErrors()) {
                ExchangeResponse.sendInternalServerError(httpServerExchange, wrapper, wrapper.getErrors());
                return;
            }

            FlexicoreHashMap flexicoreHashMap;
            flexicoreHashMap = wrapper.getSingleRecord();

            ExchangeResponse.sendFile(httpServerExchange,XmlReader.getFileUploadPath() +flexicoreHashMap.get("interviewee_id")+ File.separator + flexicoreHashMap.get("file_name"));
        } catch (Exception e) {
            ExchangeResponse.sendInternalServerError(httpServerExchange, Misc.getTransactionWrapperStackTrace(e));
        }
    }

}
