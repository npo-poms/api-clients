package nl.vpro.api.client.resteasy;

import nl.vpro.domain.classification.ClassificationService;
import nl.vpro.domain.classification.ClassificationServiceImpl;
import nl.vpro.rs.pages.update.PageUpdateRestService;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.springframework.core.io.UrlResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;
import java.net.MalformedURLException;

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
    }

    public PageUpdateRestService getPageUpdateRestService() {
        return pageUpdateRestService;
    }

    public ClassificationService getClassificationService() {

        // TODO. Should use actual Rest call?
        if (classificationService == null) {
            try {
                classificationService = new ClassificationServiceImpl(
                        new UrlResource(baseUrl + "schema/classification")
                );
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return classificationService;
    }

    @Override
    public String toString() {
        return getClass().getName() + " " + toString;
    }
}
