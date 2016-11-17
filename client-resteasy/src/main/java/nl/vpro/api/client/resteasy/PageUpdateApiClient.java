package nl.vpro.api.client.resteasy;

import lombok.Builder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.inject.Named;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import nl.vpro.domain.classification.CachedURLClassificationServiceImpl;
import nl.vpro.domain.classification.ClassificationService;
import nl.vpro.rs.pages.update.PageUpdateRestService;
import nl.vpro.util.ReflectionUtils;

public class PageUpdateApiClient extends AbstractApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(PageUpdateApiClient.class);

    private PageUpdateRestService pageUpdateRestService;

    private final String description;

    private final BasicAuthentication authentication;

    @Inject(optional = true)
    private ClassificationService classificationService;

    @Inject
    public PageUpdateApiClient(
        @Named("pageupdate-api.baseUrl") String baseUrl,
        @Named("pageupdate-api.user") String user,
        @Named("pageupdate-api.password") String password,
        @Named("pageupdate-api.connectionTimeout") int connectionTimeout
    ) {
        this(baseUrl, Duration.ofMillis(connectionTimeout), Duration.ofMillis(connectionTimeout), Duration.ofMillis(connectionTimeout), 16, Duration.ofMillis(10000), Duration.ofMinutes(15), null, null, user, password);
    }

    @Builder
    public PageUpdateApiClient(
        String baseUrl,
        Duration connectionRequestTimeout,
        Duration connectTimeout,
        Duration socketTimeout,
        int maxConnections,
        Duration connectionInPoolTTL,
        Duration countWindow,
        List<Locale> acceptableLanguages,
        Boolean trustAll,
        String user,
        String password
        ) {
        super(baseUrl + "api", connectionRequestTimeout, connectTimeout, socketTimeout, maxConnections, connectionInPoolTTL, countWindow, acceptableLanguages, null,  trustAll);
        authentication = new BasicAuthentication(user, password);
        description = user + "@" + baseUrl + "api";
    }

    public static PageUpdateApiClientBuilder configured(String... configFiles) throws IOException {
        PageUpdateApiClientBuilder builder = builder();
        LOG.info("Reading configuration from {}", Arrays.asList(configFiles));
        ReflectionUtils.configured(builder, configFiles);
        return builder;
    }

    public static PageUpdateApiClientBuilder configured() throws IOException {
        return configured(System.getProperty("user.home") + File.separator + "conf" + File.separator + "pageupdateapiclient.properties");
    }

    public PageUpdateRestService getPageUpdateRestService() {
        if (pageUpdateRestService == null) {
            pageUpdateRestService =
                proxyErrorsAndCount(
                    PageUpdateRestService.class,
                    getTarget(getClientHttpEngine())
                        .proxyBuilder(PageUpdateRestService.class)
                        .defaultConsumes(MediaType.APPLICATION_XML).build(),
                    Error.class
                );
        }
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

    @Override
    protected void buildResteasy(ResteasyClientBuilder builder) {
        builder.register(authentication);
    }


    @Override
    public synchronized void invalidate() {
        super.invalidate();
        pageUpdateRestService = null;
    }


}
