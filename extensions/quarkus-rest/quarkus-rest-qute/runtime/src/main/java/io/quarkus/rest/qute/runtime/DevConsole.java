package io.quarkus.rest.qute.runtime;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.yaml.snakeyaml.Yaml;

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
        Template devTemplate = readTemplate("/dev-templates/index.html");
        Enumeration<URL> extensionDescriptors = getClass().getClassLoader().getResources("/META-INF/quarkus-extension.yaml");
        List<Map<String, Object>> extensions = new ArrayList<>();
        Yaml yaml = new Yaml();
        while (extensionDescriptors.hasMoreElements()) {
            URL extensionDescriptor = extensionDescriptors.nextElement();
            String desc = readURL(extensionDescriptor);
            Map<String, Object> loaded = yaml.load(desc);
            String artifactId = (String) loaded.get("artifact-id");
            URL extensionSimple = getClass().getClassLoader().getResource("/dev-templates/" + artifactId + ".html");
            if (extensionSimple != null) {
                Template template = readTemplate(extensionSimple);
                String result = template.render();
                loaded.put("_dev", result);
            }
            extensions.add(loaded);
        }
        Collections.sort(extensions, (a, b) -> {
            return ((String) a.get("name")).compareTo((String) b.get("name"));
        });
        return devTemplate.data("extensions", extensions);
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
        return engine.parse(readURL(url));
    }

    private String readURL(URL url) throws IOException {
        try (Scanner scanner = new Scanner(url.openStream(),
                StandardCharsets.UTF_8.toString())) {
            scanner.useDelimiter("\\A");
            String templateBody = scanner.hasNext() ? scanner.next() : null;
            return templateBody;
        }
    }
}
