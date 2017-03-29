package nl.vpro.api.client.resteasy;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Named;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import com.google.inject.Inject;

import nl.vpro.domain.classification.CachedURLClassificationServiceImpl;
import nl.vpro.domain.classification.ClassificationService;
import nl.vpro.rs.pages.update.PageUpdateRestService;
import nl.vpro.util.Env;
import nl.vpro.util.ReflectionUtils;
import nl.vpro.util.TimeUtils;

@Slf4j
public class PageUpdateApiClient extends AbstractApiClient {

    private PageUpdateRestService pageUpdateRestService;

    private final String description;

    private final BasicAuthentication authentication;

    @Inject(optional = true)
    private ClassificationService classificationService;

    @SuppressWarnings("SpringAutowiredFieldsWarningInspection")
    @Named
    public static class Builder implements javax.inject.Provider<PageUpdateApiClient> {

        @Inject@Named("pageupdate-api.baseUrl")
        String baseUrl;
        @Inject@Named("pageupdate-api.user")
        String user;
        @Inject@Named("pageupdate-api.password")
        String password;
        @Inject@Named("pageupdate-api.connectionRequestTimeout")
        String _connectionRequestTimeout;
        @Inject@Named("pageupdate-api.connectTimeout") String _connectTimeout;
        @Inject@Named("pageupdate-api.socketTimeout")
        String _socketTimeout;
        @Inject@Named("pageupdate-api.maxConnections")
        Integer maxConnections;
        @Inject@Named("pageupdate-api.maxConnectionsPerRoute")
        Integer maxConnectionsPerRoute;
        @Inject@Named("pageupdate-api.warnTreshold")
        String warnTreshold;


        @Override
        public PageUpdateApiClient get() {
            connectionRequestTimeout(TimeUtils.parseDuration(_connectionRequestTimeout).orElseThrow(IllegalArgumentException::new));
            connectTimeout(TimeUtils.parseDuration(_connectTimeout).orElseThrow(IllegalArgumentException::new));
            socketTimeout(TimeUtils.parseDuration(_socketTimeout).orElseThrow(IllegalArgumentException::new));
            return build();
        }
    }

    @lombok.Builder(builderClassName = "Builder")
    protected PageUpdateApiClient(
        String baseUrl,
        Duration connectionRequestTimeout,
        Duration connectTimeout,
        Duration socketTimeout,
        Integer maxConnections,
        Integer maxConnectionsPerRoute,
        Duration connectionInPoolTTL,
        Duration countWindow,
        Integer bucketCount,
        Duration warnTreshold,
        List<Locale> acceptableLanguages,
        Boolean trustAll,
        String user,
        String password
        ) {
        super(baseUrl + (baseUrl.endsWith("/") ?  "" : "/") + "api",
            connectionRequestTimeout,
            connectTimeout,
            socketTimeout,
            maxConnections,
            maxConnectionsPerRoute,
            connectionInPoolTTL,
            countWindow,
            bucketCount,
            warnTreshold,
            acceptableLanguages,
            null,
            trustAll);
        if (user == null){
            throw new IllegalArgumentException("No user given");
        }
        if (password == null) {
            throw new IllegalArgumentException("No  password given");
        }
        authentication = new BasicAuthentication(user, password);
        description = user + "@" + this.getBaseUrl();
    }

    public static Builder configured(String... configFiles) throws IOException {
        Builder builder = builder();
        log.info("Reading configuration from {}", Arrays.asList(configFiles));
        ReflectionUtils.configured(builder, configFiles);
        return builder;
    }

    public static Builder configured(Env env, String... configFiles) {
        Builder  builder = builder();
        ReflectionUtils.configured(env, builder, configFiles);
        return builder;
    }

    public static Builder configured(Env env, Map<String, String> settings) {
        Builder  builder = builder();
        ReflectionUtils.configured(env, builder, settings);
        return builder;
    }


    public static Builder configured() throws IOException {
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
                log.info("No classification service wired. Created {}", this.classificationService);

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
