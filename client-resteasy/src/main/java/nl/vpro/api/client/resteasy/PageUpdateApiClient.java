package nl.vpro.api.client.resteasy;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Duration;
import java.util.*;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import nl.vpro.domain.classification.CachedURLClassificationServiceImpl;
import nl.vpro.domain.classification.ClassificationService;
import nl.vpro.rs.pages.update.PageUpdateRestService;
import nl.vpro.util.Env;
import nl.vpro.util.ProviderAndBuilder;
import nl.vpro.util.ReflectionUtils;

@Slf4j
public class PageUpdateApiClient extends AbstractApiClient {


    private PageUpdateRestService pageUpdateRestService;

    private final String description;

    private final BasicAuthentication authentication;

    private ClassificationService classificationService;

    @SuppressWarnings({"SpringAutowiredFieldsWarningInspection", "OptionalUsedAsFieldOrParameterType"})
    @Named
    public static class Provider implements javax.inject.Provider<PageUpdateApiClient> {

        @Inject
        @Named("npo-pageupdate-api.baseUrl")
        String baseUrl;
        @Inject
        @Named("npo-pageupdate-api.user")
        String user;
        @Inject
        @Named("npo-pageupdate-api.password")
        String password;
        @Inject
        @Named("npo-pageupdate-api.connectionRequestTimeout")
        Optional<String> connectionRequestTimeout;
        @Inject
        @Named("npo-pageupdate-api.connectTimeout")
        Optional<String> connectTimeout;
        @Inject
        @Named("npo-pageupdate-api.socketTimeout")
        Optional<String> socketTimeout;
        @Inject
        @Named("npo-pageupdate-api.maxConnections")
        Optional<Integer> maxConnections;
        @Inject
        @Named("npo-pageupdate-api.maxConnectionsPerRoute")
        Optional<Integer> maxConnectionsPerRoute;
        @Inject
        @Named("npo-pageupdate-api.warnThreshold")
        Optional<String> warnThreshold;
        // should have worked, but at least I couldn't get it working in magnolia. I made a duration convertor in ProviderAndBuilder now.
        // Optional<Duration> warnThreshold;

        @Inject
        private Optional<ClassificationService> classificationService;


        @Override
        public PageUpdateApiClient get() {
            return ProviderAndBuilder.fillAndCatch(this, builder()).build();
        }
    }

    public static class Builder {

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
        Duration warnThreshold,
        List<Locale> acceptableLanguages,
        Boolean trustAll,
        String user,
        String password,
        String mbeanName,
        ClassificationService classificationService
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
            warnThreshold,
            acceptableLanguages,
            null,
            null,
            trustAll,
            null,
            mbeanName);
        if (user == null){
            throw new IllegalArgumentException("No user given");
        }
        if (password == null) {
            throw new IllegalArgumentException("No  password given");
        }
        authentication = new BasicAuthentication(user, password);
        description = user + "@" + this.getBaseUrl();
        this.classificationService = classificationService;
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
        return pageUpdateRestService = produceIfNull(
            () -> pageUpdateRestService,
            () -> proxyErrorsAndCount(
                PageUpdateRestService.class,
                    getTarget(getClientHttpEngine())
                        .proxyBuilder(PageUpdateRestService.class)
                        .defaultConsumes(MediaType.APPLICATION_XML).build(),
                Error.class
            ));
    }

    public ClassificationService getClassificationService() {
        return classificationService = produceIfNull(
            () -> classificationService,
            () -> {
                try {

                    log.info("No classification service wired. Created {}", this.classificationService);
                    return new CachedURLClassificationServiceImpl(this.baseUrl);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            });
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
