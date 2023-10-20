package ke.co.survey.rest.middleware;

import io.undertow.server.HttpServerExchange;
import ke.co.skyworld.ancillaries.restful_undertow.handlers.ExchangeMiddleware;
import ke.co.skyworld.ancillaries.restful_undertow.interfaces.ExchangeHandler;


public class HandlerAuthorizationMiddleware extends ExchangeMiddleware {
    private final ExchangeHandler nextHandler;

    public HandlerAuthorizationMiddleware(ExchangeHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange, boolean[] breakChain, Object... otherParams) throws Exception {
        //LOGIC
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
