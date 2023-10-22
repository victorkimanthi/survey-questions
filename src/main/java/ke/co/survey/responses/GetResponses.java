package ke.co.survey.responses;

import io.undertow.server.HttpServerExchange;
import ke.co.skyworld.ancillaries.query_manager.beans.FlexicoreArrayList;
import ke.co.skyworld.ancillaries.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.ancillaries.query_manager.beans.PageableWrapper;
import ke.co.skyworld.ancillaries.query_manager.beans.TransactionWrapper;
import ke.co.skyworld.ancillaries.query_manager.query.FilterPredicate;
import ke.co.skyworld.ancillaries.query_manager.query.FilterTokenizer;
import ke.co.skyworld.ancillaries.query_repository.Repository;
import ke.co.skyworld.ancillaries.restful_undertow.authentication_handlers.ExchangeContextAuthenticated;
import ke.co.skyworld.ancillaries.restful_undertow.beans.UserSessionContext;
import ke.co.skyworld.ancillaries.restful_undertow.utils.ExchangeResponse;
import ke.co.skyworld.ancillaries.restful_undertow.utils.ExchangeUtils;
import ke.co.skyworld.ancillaries.utility_items.Misc;
import ke.co.skyworld.ancillaries.utility_items.constants.StringRefs;

import java.util.*;

public class GetResponses extends ExchangeContextAuthenticated {
    @Override
    public void handleRequest(HttpServerExchange httpServerExchange, UserSessionContext userSessionContext, Object... objects) {

        FlexicoreArrayList resultArrayList;
        TransactionWrapper wrapper2;
        String organizationId = StringRefs.SENTINEL;

        String [] parameterKeys= {"page","pageSize","email"};

        int[] pageNoPageSize =new int[2];
        int pageSize = 0;
        int page = 0;

        try {

            HashMap<String,String> parameters = ExchangeUtils.getQueryParams(httpServerExchange,parameterKeys);
            System.out.println("parameters:"+ parameters);

            if(parameters.containsKey("page")) {
                page =  Integer.parseInt(parameters.get("page"));
                pageNoPageSize[0] =page;
            }

            if(parameters.containsKey("pageSize")) {
                pageSize = Integer.parseInt(parameters.get("pageSize"));
                pageNoPageSize[1] =pageSize;
            }

            String email = parameters.get("email");

            System.out.println("email:"+email);

            long totalRecords = Repository.count(organizationId, "responses");

            System.out.println("totalRecords:"+totalRecords);
            System.out.println("pageSize:"+pageSize);
            System.out.println("page:"+page);

            //TOTAL PAGES IS EQUAL TO THE LAST PAGE
            int totalPages = 0;
            if(pageSize != 0) {
                 totalPages = (int) ((totalRecords + pageSize - 1) / pageSize);
            }

            System.out.println("totalPages:"+totalPages);

           /* if(true){
                return;
            }*/

            String filterString = userSessionContext.getFilter();
            System.out.println("filterString:"+filterString);

            if(filterString == null) {

                 wrapper2 = Repository.selectWhereOrderBy(organizationId, "responses","DISTINCT interviewee_id,full_name,gender,email_address,description,frontend_stack,certificates,date_created as date_responded",
                         new FilterPredicate(" email_address = COALESCE(:email_address, email_address)"),"date_created DESC",
                         new FlexicoreHashMap().addQueryArgument(":email_address",email),pageNoPageSize);

                if(wrapper2.hasErrors()){
                    ExchangeResponse.sendInternalServerError(httpServerExchange,wrapper2.getErrors());
                    return;
                }

                System.out.println(" wrapper2.getData():" +  wrapper2.getData());

            }else {
                TransactionWrapper<FilterTokenizer.ParsedFilterData> filterTWrapper = FilterTokenizer.generatePredicate(filterString);

                if (filterTWrapper.hasErrors()) {
                    ExchangeResponse.sendInternalServerError(httpServerExchange, filterTWrapper.getErrors());
                    return;
                }

                 wrapper2 = Repository.selectWhereOrderBy(organizationId, "responses","DISTINCT full_name,gender,email_address,description,frontend_stack,certificates,date_created as date_responded",
                        filterTWrapper.getData().filterPredicate.customFilter("OR email_address = :email_address"),"date_created DESC",
                        filterTWrapper.getData().queryArguments.addQueryArgument(":email_address",email),pageNoPageSize);

                System.out.println("wrapper2:" + wrapper2.getData());
            }

            if (wrapper2.getData() instanceof PageableWrapper<?>) {
                //PageableWrapper contains the data and also includes more details like the page and page size. You can send it as a response to an API call.
                PageableWrapper<FlexicoreArrayList> pageableWrapper = (PageableWrapper<FlexicoreArrayList>) wrapper2.getData();
                resultArrayList = pageableWrapper.getData();
            } else {
                resultArrayList = (FlexicoreArrayList) wrapper2.getData();
            }

            if (wrapper2.hasErrors()) {
                ExchangeResponse.sendInternalServerError(httpServerExchange,wrapper2.getErrors());
                return;
            }

            System.out.println("request body is:" + Arrays.toString(pageNoPageSize));

            if (resultArrayList.isEmpty()) {
                ExchangeResponse.sendNotFound(httpServerExchange,"No records","There were no records found.");
                return;
            }

            System.out.println("resultArrayList:"+resultArrayList);
            FlexicoreHashMap resultsHashMap = new FlexicoreHashMap();
            resultsHashMap.put("current_page",page);
            resultsHashMap.put("last_page",totalPages);
            resultsHashMap.put("page_size",pageSize);
            resultsHashMap.put("data",resultArrayList);

            //sending  response that records have been fetched successfully
            ExchangeResponse.sendOK(httpServerExchange, resultsHashMap);
        } catch (Exception e) {
            ExchangeResponse.sendInternalServerError(httpServerExchange, Misc.getTransactionWrapperStackTrace(e));
        }
    }

}
