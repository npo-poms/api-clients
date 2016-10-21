package nl.vpro.api.client.resteasy;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;

import javax.inject.Named;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import nl.vpro.domain.classification.CachedURLClassificationServiceImpl;
import nl.vpro.domain.classification.ClassificationService;
import nl.vpro.resteasy.JacksonContextResolver;
import nl.vpro.rs.pages.update.PageUpdateRestService;
import nl.vpro.util.ReflectionUtils;

import static nl.vpro.api.client.resteasy.ErrorAspect.proxyErrors;

public class PageUpdateApiClient extends AbstractApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(PageUpdateApiClient.class);

    private PageUpdateRestService pageUpdateRestService;

    private final String description;

    private final BasicAuthentication authentication;

    @Inject(optional = true)
    private ClassificationService classificationService;

    @Inject
    public PageUpdateApiClient(
        @Named("pageupdate-api.baseUrl") String apiBaseUrl,
        @Named("pageupdate-api.user") String user,
        @Named("pageupdate-api.password") String password,
        @Named("pageupdate-api.connectionTimeout") int connectionTimeout
    ) {
        super(apiBaseUrl + "api", connectionTimeout, 16, 10000);
        authentication = new BasicAuthentication(user, password);
        description = user + "@" + apiBaseUrl;

    }

    public static Builder configured(String... configFiles) throws IOException {
        PageUpdateApiClient.Builder builder = new PageUpdateApiClient.Builder();
        LOG.info("Reading configuration from {}", Arrays.asList(configFiles));
        ReflectionUtils.configured(builder, configFiles);
        return builder;
    }

    public static Builder configured() throws IOException {
        return configured(
            System.getProperty("user.home") +
            File.separator + "conf" +
            File.separator + "pageupdateapiclient.properties");
    }

    public PageUpdateRestService getPageUpdateRestService() {
        if (pageUpdateRestService == null) {
            pageUpdateRestService =
                proxyErrors(LOG,
                    PageUpdateApiClient.this::getBaseUrl,
                    PageUpdateRestService.class,
                    getTarget(getClientHttpEngine())
                        .proxyBuilder(PageUpdateRestService.class).defaultConsumes(MediaType.APPLICATION_XML)
                        .build(),
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
    protected ResteasyWebTarget getTarget(ClientHttpEngine clientHttpEngine) {
        ResteasyClient client = new ResteasyClientBuilder()
            .httpEngine(getClientHttpEngine())
            .register(authentication)
            .register(JacksonContextResolver.class)
            .build();
        return client.target(baseUrl);
    }


    public static class Builder {
        private String apiBaseUrl;
        private String user;
        private String password;
        private int connectionTimeout = 10000;


        public PageUpdateApiClient build() {
            return new PageUpdateApiClient(apiBaseUrl, user, password, connectionTimeout);
        }

        public String getApiBaseUrl() {
            return apiBaseUrl;
        }

        public Builder setApiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
            return this;
        }

        public String getUser() {
            return user;
        }

        public Builder setUser(String user) {
            this.user = user;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public int getConnectionTimeout() {
            return connectionTimeout;
        }

        public Builder setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }
    }
}
