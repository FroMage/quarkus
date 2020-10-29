package io.quarkus.rest.qute.runtime;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

@Path("/@dev")
public class DevConsole {
    @Inject
    Engine engine;

    @Path("/")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance hello() throws IOException {
        Enumeration<URL> devTemplates = getClass().getClassLoader().getResources("/dev-templates/simple.html");
        List<String> templates = new ArrayList<>();
        while (devTemplates.hasMoreElements()) {
            URL devTemplate = devTemplates.nextElement();
            Template template = readTemplate(devTemplate);
            String result = template.render();
            templates.add(result);
        }
        Template devTemplate = readTemplate("/dev-templates/index.html");
        return devTemplate.data("simples", templates);
    }

    @Path("{path:.+}")
    @GET
    public Response get(String path) throws IOException {
        URL url = getClass().getClassLoader().getResource("/dev-templates/" + path + ".html");
        if (url != null) {
            Template template = readTemplate(url);
            return Response.ok(template.instance(), MediaType.TEXT_HTML_TYPE).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    private Template readTemplate(String path) throws IOException {
        URL url = getClass().getClassLoader().getResource(path);
        return readTemplate(url);
    }

    private Template readTemplate(URL url) throws IOException {
        try (Scanner scanner = new Scanner(url.openStream(),
                StandardCharsets.UTF_8.toString())) {
            scanner.useDelimiter("\\A");
            String templateBody = scanner.hasNext() ? scanner.next() : null;
            return engine.parse(templateBody);
        }
    }
}
