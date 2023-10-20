package ke.co.survey.questions;

import io.undertow.server.HttpServerExchange;
import ke.co.skyworld.ancillaries.query_manager.beans.FlexicoreArrayList;
import ke.co.skyworld.ancillaries.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.ancillaries.query_manager.beans.PageableWrapper;
import ke.co.skyworld.ancillaries.query_manager.beans.TransactionWrapper;
import ke.co.skyworld.ancillaries.query_manager.query.FilterPredicate;
import ke.co.skyworld.ancillaries.query_repository.Repository;
import ke.co.skyworld.ancillaries.restful_undertow.authentication_handlers.ExchangeContextAuthenticated;
import ke.co.skyworld.ancillaries.restful_undertow.beans.UserSessionContext;
import ke.co.skyworld.ancillaries.restful_undertow.interfaces.ExchangeHandler;
import ke.co.skyworld.ancillaries.restful_undertow.utils.ExchangeResponse;
import ke.co.skyworld.ancillaries.restful_undertow.utils.ExchangeUtils;
import ke.co.skyworld.ancillaries.utility_items.Misc;
import ke.co.skyworld.ancillaries.utility_items.constants.StringRefs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GetQuestions implements ExchangeHandler {
    @Override
    public void handleRequest(HttpServerExchange httpServerExchange, Object... objects) {

        try {
            TransactionWrapper <FlexicoreHashMap> wrapper = Repository.selectWhere(StringRefs.SENTINEL, "questions",
                    new FilterPredicate("questions_id = :questions_id"),
                    new FlexicoreHashMap().addQueryArgument(":questions_id", 38));

            if (wrapper.hasErrors()) {
                ExchangeResponse.sendInternalServerError(httpServerExchange, wrapper, "Error occurred while getting questions.");
                return;
            }
//
            FlexicoreHashMap resultsMap = new FlexicoreHashMap();

            resultsMap.put("data",wrapper.getSingleRecord());
            //sending  response that records have been fetched successfully
            ExchangeResponse.sendOK(httpServerExchange,resultsMap);

        } catch (Exception e) {
            ExchangeResponse.sendInternalServerError(httpServerExchange, Misc.getTransactionWrapperStackTrace(e));
        }
    }
}
