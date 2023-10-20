package ke.co.survey.rest.middleware;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import ke.co.skyworld.ancillaries.restful_undertow.handlers.ExchangeMiddleware;
import ke.co.skyworld.ancillaries.restful_undertow.interfaces.ExchangeHandler;
import ke.co.skyworld.ancillaries.restful_undertow.utils.ExchangeResponse;

public class HandlerAuthHeadersValidator extends ExchangeMiddleware {

    private final ExchangeHandler nextHandler;

    public HandlerAuthHeadersValidator(ExchangeHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange, boolean[] breakChain, Object... otherParams) throws Exception {
        HeaderValues authHeader = exchange.getRequestHeaders().get("Authorization");
//        HeaderValues requestReferenceHeader = exchange.getRequestHeaders().get("RequestReference");

        if (authHeader == null) {
            ExchangeResponse.sendBadRequest(exchange, "Missing HEADER 'Authorization'", "HEADER 'Authorization' is required");
            breakChain[0] = true;
            return;
        }

        /*if (requestReferenceHeader == null) {
            ExchangeResponse.sendBadRequest(exchange, "Missing HEADER 'RequestReference'", "HEADER 'RequestReference' is required");
            breakChain[0] = true;
            return;
        }

        String reqReference = requestReferenceHeader.getFirst();
        if (reqReference.isEmpty()) {
            ExchangeResponse.sendBadRequest(exchange, "Missing value for 'RequestReference' in HEADER", "RequestReference in HEADER cannot be empty");
            breakChain[0] = true;
            return;
        }

        String accessToken = authHeader.getFirst().replace("Bearer ", "");
        if (accessToken.isEmpty()) {
            ExchangeResponse.sendBadRequest(exchange, "Missing value for 'Authorization' in HEADER", "Authorization in HEADER cannot be empty");
            breakChain[0] = true;
            return;
        }
*/
        breakChain[0] = false;

        if (nextHandler instanceof ExchangeMiddleware middleware) {
            middleware.handleRequest(exchange, breakChain, (Object) null);
        } else {
            nextHandler.handleRequest(exchange);
        }
    }

    @Override
    public ExchangeHandler getNextHandler() {
        return nextHandler;
    }
}
