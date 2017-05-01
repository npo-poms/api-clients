package nl.vpro.api.client.resteasy;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Duration;
import java.util.*;

import javax.inject.Named;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import com.google.inject.Inject;

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

    @Inject(optional = true)
    private ClassificationService classificationService;

    @SuppressWarnings({"SpringAutowiredFieldsWarningInspection", "OptionalUsedAsFieldOrParameterType"})
    @Named
    public static class Provider implements javax.inject.Provider<PageUpdateApiClient> {

        @Inject@Named("npo-pageupdate-api.baseUrl")
        String baseUrl;
        @Inject@Named("npo-pageupdate-api.user")
        String user;
        @Inject@Named("npo-pageupdate-api.password")
        String password;
        @Inject@Named("npo-pageupdate-api.connectionRequestTimeout")
        Optional<Duration> connectionRequestTimeout;
        @Inject@Named("npo-pageupdate-api.connectTimeout")
        Optional<Duration> connectTimeout;
        @Inject@Named("npo-pageupdate-api.socketTimeout")
        Optional<Duration> socketTimeout;
        @Inject@Named("npo-pageupdate-api.maxConnections")
        Optional<Integer> maxConnections;
        @Inject@Named("npo-pageupdate-api.maxConnectionsPerRoute")
        Optional<Integer> maxConnectionsPerRoute;
        @Inject@Named("npo-pageupdate-api.warnThreshold")
        Optional<Duration>  warnThreshold;


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
        String mbeanName
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
