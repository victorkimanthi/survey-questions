package ke.co.survey.rest;

import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.form.EagerFormParsingHandler;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.server.handlers.form.MultiPartParserDefinition;
import io.undertow.util.Methods;
import ke.co.skyworld.ancillaries.restful_undertow.handlers.HttpHandlerMiddleware;
import ke.co.skyworld.ancillaries.restful_undertow.handlers.RequestLogHandler;
import ke.co.skyworld.ancillaries.restful_undertow.interfaces.ExchangeHandler;
import ke.co.skyworld.ancillaries.undertow_rest.layers.CorsHandler;
import ke.co.skyworld.ancillaries.undertow_rest.layers.Dispatcher;
import ke.co.skyworld.ancillaries.undertow_rest.layers.FallBack;
import ke.co.skyworld.ancillaries.undertow_rest.layers.InvalidMethod;
import ke.co.survey.questions.AddResponse;
import ke.co.survey.questions.GetQuestions;
import ke.co.survey.responses.GetAttachedFile;
import ke.co.survey.responses.GetIndividualResponse;
import ke.co.survey.responses.GetResponses;
import ke.co.survey.rest.middleware.HandlerAuthHeadersValidator;


public class Routes {
    public static RoutingHandler questions() {
        return Handlers.routing()
                .post("",handlerNoAuthUpload(new AddResponse()))
                .get("/responses",handlerNoAuthBlocking(new GetResponses()))
//                .get("/responses/{intervieweeId}",handlerNoAuthBlocking(new GetIndividualResponse()))
                .get("/response",handlerNoAuthBlocking(new GetIndividualResponse()))
                .get("/responses/download",handlerNoAuthBlocking(new GetAttachedFile()))
                .get("", handlerNoAuthBlocking(new GetQuestions()))
                .add(Methods.OPTIONS, "/*", new CorsHandler())
                .setInvalidMethodHandler(new Dispatcher(new InvalidMethod()))
                .setFallbackHandler(new Dispatcher(new FallBack()));
    }
    private static HttpHandler handlerNoAuthBlocking(ExchangeHandler exchangeHandler) {
        HandlerAuthHeadersValidator handlerAuthHeadersValidator = new HandlerAuthHeadersValidator(exchangeHandler);
        return new BlockingHandler(new HttpHandlerMiddleware(new RequestLogHandler(),handlerAuthHeadersValidator));
    }
    private static HttpHandler handlerNoAuthUpload(ExchangeHandler exchangeHandler) {
        HandlerAuthHeadersValidator handlerAuthHeadersValidator = new HandlerAuthHeadersValidator(exchangeHandler);
        return uploadHandler(new HttpHandlerMiddleware(new RequestLogHandler(),handlerAuthHeadersValidator));
    }
    private static EagerFormParsingHandler uploadHandler(HttpHandler next) {
        return new EagerFormParsingHandler(
                FormParserFactory
                        .builder()
                        .addParser(new MultiPartParserDefinition())
                        .build()
        ).setNext(next);
    }
}
