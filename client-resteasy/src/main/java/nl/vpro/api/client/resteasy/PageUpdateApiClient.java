package nl.vpro.api.client.resteasy;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.UrlResource;

import nl.vpro.domain.classification.ClassificationService;
import nl.vpro.domain.classification.URLClassificationServiceImpl;
import nl.vpro.rs.pages.update.PageUpdateRestService;

public class PageUpdateApiClient extends AbstractApiClient {
    private final BasicAuthentication authentication;

    private final PageUpdateRestService pageUpdateRestService;

    private final String toString;

    private final String baseUrl;

    private ClassificationService classificationService;

    @Inject
    public PageUpdateApiClient(
        @Named("pageupdate-api.baseUrl") String apiBaseUrl,
        @Named("pageupdate-api.user") String user,
        @Named("pageupdate-api.password") String password
    ) throws MalformedURLException {
        super(10000, 16, 10000);
        this.authentication = new BasicAuthentication(user, password);
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(clientHttpEngine).register(authentication).build();
        ResteasyWebTarget target = client.target(apiBaseUrl + "api/");
        pageUpdateRestService = target.proxyBuilder(PageUpdateRestService.class).defaultConsumes(MediaType.APPLICATION_XML).build();
        toString = user + "@" + apiBaseUrl;
        this.baseUrl = apiBaseUrl;
        this.classificationService = new URLClassificationServiceImpl(new URL(this.baseUrl + "schema/classification/"));
    }

    public PageUpdateRestService getPageUpdateRestService() {
        return pageUpdateRestService;
    }

    public ClassificationService getClassificationService() {
        return classificationService;
    }

    @Override
    public String toString() {
        return getClass().getName() + " " + toString;
    }
}
