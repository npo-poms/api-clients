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
import nl.vpro.resteasy.JacksonContextResolver;
import nl.vpro.rs.pages.update.PageUpdateRestService;

import static nl.vpro.api.client.resteasy.ErrorAspect.proxyErrors;

public class PageUpdateApiClient extends AbstractApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(PageUpdateApiClient.class);

    private final PageUpdateRestService pageUpdateRestService;

    private final String description;

    private final String baseUrl;

    @Inject(optional = true)
    private ClassificationService classificationService;

    @Inject
    public PageUpdateApiClient(
        @Named("pageupdate-api.baseUrl") String apiBaseUrl,
        @Named("pageupdate-api.user") String user,
        @Named("pageupdate-api.password") String password,
        @Named("pageupdate-api.connectionTimeout") int connectionTimeout
    ) {
        super(connectionTimeout, 16, 10000);
        BasicAuthentication authentication = new BasicAuthentication(user, password);
        ResteasyClient client = new ResteasyClientBuilder()
            .httpEngine(clientHttpEngine)
            .register(authentication)
            .register(JacksonContextResolver.class)
            .build();
        ResteasyWebTarget target = client.target(apiBaseUrl + "api/");
        pageUpdateRestService =
            proxyErrors(LOG,
                PageUpdateApiClient.this::getBaseUrl,
                PageUpdateRestService.class,
                target
                    .proxyBuilder(PageUpdateRestService.class).defaultConsumes(MediaType.APPLICATION_XML)
                    .build(),
                Error.class
            );
        description = user + "@" + apiBaseUrl;
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

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return getClass().getName() + " " + getDescription();
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
