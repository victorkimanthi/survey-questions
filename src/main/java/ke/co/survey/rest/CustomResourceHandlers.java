package ke.co.survey.rest;

import io.undertow.Handlers;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Paths;

public class CustomResourceHandlers {

    public static ResourceHandler getPortal() {
        String path = System.getProperty("user.dir") + "/portal/build";
        //String path = "/home/kitchain/TEMP/doc_build/build";
        try {
            path = URLDecoder.decode(path, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            path = path.replace("%20"," ");
        }

        return Handlers.resource(new PathResourceManager(Paths.get(path),100))
                .addWelcomeFiles("index.html").setDirectoryListingEnabled(true);
    }
}