package nl.vpro.api.client.resteasy;

import lombok.Builder;
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
import nl.vpro.util.ReflectionUtils;
import nl.vpro.util.TimeUtils;

@Slf4j
public class PageUpdateApiClient extends AbstractApiClient {

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
        @Named("pageupdate-api.connectionRequestTimeout") String connectionRequestTimeout,
        @Named("pageupdate-api.connectTimeout") String connectTimeout,
        @Named("pageupdate-api.socketTimeout") String socketTimeout,
        @Named("pageupdate-api.maxConnections") int maxConnections,
        @Named("pageupdate-api.maxConnectionsPerRoute") int maxConnectionsPerRoute) {
        this(baseUrl,
            TimeUtils.parseDuration(connectionRequestTimeout).orElseThrow(IllegalArgumentException::new),
            TimeUtils.parseDuration(connectTimeout).orElseThrow(IllegalArgumentException::new),
            TimeUtils.parseDuration(socketTimeout).orElseThrow(IllegalArgumentException::new),
            maxConnections,
            maxConnectionsPerRoute,
            Duration.ofSeconds(10),
            Duration.ofMinutes(15),
            null,
            null,
            user, password);
    }

    @Builder
    public PageUpdateApiClient(
        String baseUrl,
        Duration connectionRequestTimeout,
        Duration connectTimeout,
        Duration socketTimeout,
        int maxConnections,
        int maxConnectionsPerRoute,
        Duration connectionInPoolTTL,
        Duration countWindow,
        List<Locale> acceptableLanguages,
        Boolean trustAll,
        String user,
        String password
        ) {
        super(baseUrl + (baseUrl.endsWith("/") ?  "" : "/") + "api", connectionRequestTimeout, connectTimeout, socketTimeout, maxConnections, maxConnectionsPerRoute, connectionInPoolTTL, countWindow, acceptableLanguages, null,  trustAll);
        if (user == null){
            throw new IllegalArgumentException("No user given");
        }
        if (password == null) {
            throw new IllegalArgumentException("No  password given");
        }
        authentication = new BasicAuthentication(user, password);
        description = user + "@" + this.getBaseUrl();
    }

    public static PageUpdateApiClientBuilder configured(String... configFiles) throws IOException {
        PageUpdateApiClientBuilder builder = builder();
        log.info("Reading configuration from {}", Arrays.asList(configFiles));
        ReflectionUtils.configured(builder, configFiles);
        return builder;
    }

    public static PageUpdateApiClientBuilder configured(Env env, String... configFiles) {
        PageUpdateApiClientBuilder  builder = builder();
        ReflectionUtils.configured(env, builder, configFiles);
        return builder;
    }

    public static PageUpdateApiClientBuilder configured(Env env, Map<String, String> settings) {
        PageUpdateApiClientBuilder  builder = builder();
        ReflectionUtils.configured(env, builder, settings);
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
