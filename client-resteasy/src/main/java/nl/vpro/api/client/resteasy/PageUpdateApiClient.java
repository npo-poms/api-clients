package nl.vpro.api.client.resteasy;

import java.net.MalformedURLException;

import javax.inject.Named;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import nl.vpro.domain.classification.CachedURLClassificationServiceImpl;
import nl.vpro.domain.classification.ClassificationService;
import nl.vpro.rs.pages.update.PageUpdateRestService;

public class PageUpdateApiClient extends AbstractApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(PageUpdateApiClient.class);


    private final BasicAuthentication authentication;

    private final PageUpdateRestService pageUpdateRestService;

    private final String toString;

    private final String baseUrl;

    @Inject(optional = true)
    private ClassificationService classificationService;

    @Inject
    public PageUpdateApiClient(
        @Named("pageupdate-api.baseUrl") String apiBaseUrl,
        @Named("pageupdate-api.user") String user,
        @Named("pageupdate-api.password") String password
    ) {
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
        if (classificationService == null) {
            try {
                this.classificationService = new CachedURLClassificationServiceImpl(this.baseUrl);
                LOG.info("No classification service wired. Created {}", this.classificationService);

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
