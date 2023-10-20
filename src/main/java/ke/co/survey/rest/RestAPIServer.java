package ke.co.survey.rest;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.sse.ServerSentEventHandler;
import ke.co.skyworld.ancillaries.configs.ConfigFile;
import ke.co.skyworld.ancillaries.restful_undertow.handlers.CORSHandler;
import org.fusesource.jansi.Ansi;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import static io.undertow.Handlers.serverSentEvents;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * sapple (ke.co.codecypress.api.rest)
 * Created by: elon
 * On: 28 Jul, 2019 28/07/19 20:05
 **/
public class RestAPIServer {

    private static Undertow server;

    private static final String BASE_REST_API_URL = ConfigFile.getRestAPIBaseURL();

    private static final String BASE_PORTAL_URL = ConfigFile.getRestAPIPortalBaseURL();

    final static ServerSentEventHandler serverSentEvents = serverSentEvents();

    public static void start() {
        try {

            PathHandler pathHandler = Handlers.path()
                    //.addExactPath(BASE_PORTAL_URL+"/", Routes.portal())
                    .addPrefixPath(BASE_REST_API_URL + "/questions", Routes.questions())
                    .addPrefixPath(BASE_REST_API_URL + "/responses", Routes.responses())
                    //.addPrefixPath(BASE_REST_API_URL + "/server-sent-events", Routes.customSSEHandler())
            ;

            server = Undertow.builder()
                    .setServerOption(UndertowOptions.DECODE_URL, true)
                    .setServerOption(UndertowOptions.URL_CHARSET, StandardCharsets.UTF_8.name())
                    .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                    //.setServerOption(UndertowOptions.ALLOW_ENCODED_SLASH, true)
                    .setIoThreads(ConfigFile.getRestAPIIOThreadPool())
                    .setWorkerThreads(ConfigFile.getRestAPIWorkerThreadPool())
                    .addHttpListener(ConfigFile.getRestAPIPort(),ConfigFile.getRestAPIHost())
//                    .setHandler(new RequestLogFullDumpHandler(new CORSHandler(pathHandler)))
                    .setHandler(new CORSHandler(pathHandler))
                    .build();

            server.start();

//            System.out.println();
            System.out.println(ansi()
                    .fg(Ansi.Color.GREEN).a(" Rest API Server started at: ")
                    .bold().a(ConfigFile.getRestAPIHost() + ":" + ConfigFile.getRestAPIPort() + BASE_REST_API_URL).reset());
            System.out.println();
        }
        catch (Exception e) {
            e.printStackTrace();
            //System.exit(0);
        }
    }

    public static SSLContext serverSslContext() {
        try {

            System.setProperty("javax.net.ssl.trustStore", "NUL");
            System.setProperty("javax.net.ssl.trustStoreType", "Windows-ROOT");

            SSLContext context = SSLContext.getInstance("TLS");
            KeyManagerFactory keyFac = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore keyStore = KeyStore.getInstance("WINDOWS-MY");
            keyStore.load(null, null);
            keyFac.init(keyStore, null);
            TrustManagerFactory trustFac = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore trustStore = KeyStore.getInstance("WINDOWS-ROOT");
            trustStore.load(null, null);
            trustFac.init(trustStore);
            context.init(keyFac.getKeyManagers(), trustFac.getTrustManagers(), null);
            return context;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        /*try {
            KeyStore keyStore = loadKeyStore();
            KeyStore trustStore = loadKeyStore();
            return createSSLContext(keyStore, trustStore, password2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
    }

    public static void stop() {
        try {
            server.stop();

            System.out.println(ansi()
                    .fg(Ansi.Color.GREEN).a(" Rest API Server stopped at: ")
                    .bold().a(ConfigFile.getRestAPIHost() + ":" +
                            ConfigFile.getRestAPIPort() + BASE_REST_API_URL).reset());
            System.out.println();

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void restart() {
        stop();
        start();
    }

}
