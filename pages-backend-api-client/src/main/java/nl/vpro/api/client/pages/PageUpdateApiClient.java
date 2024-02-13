package nl.vpro.api.client.pages;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.crypto.SecretKey;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import com.google.common.base.Suppliers;

import nl.vpro.api.client.resteasy.AbstractApiClient;
import nl.vpro.api.client.utils.Config;
import nl.vpro.api.client.utils.Swagger;
import nl.vpro.domain.classification.CachedURLClassificationServiceImpl;
import nl.vpro.domain.classification.ClassificationService;
import nl.vpro.domain.page.PageIdMatch;
import nl.vpro.domain.page.update.PageUpdate;
import nl.vpro.jmx.MBeans;
import nl.vpro.logging.simple.Level;
import nl.vpro.rs.client.VersionResult;
import nl.vpro.rs.converters.ConditionalBasicAuthentication;
import nl.vpro.rs.pages.update.PageUpdateRestService;
import nl.vpro.rs.provider.ApiProviderRestService;
import nl.vpro.rs.thesaurus.update.ThesaurusUpdateRestService;
import nl.vpro.util.*;

import static nl.vpro.api.client.utils.Config.CONFIG_FILE;
import static nl.vpro.api.client.utils.Config.URLS_FILE;

@Slf4j
public class PageUpdateApiClient extends AbstractApiClient {

    private PageUpdateRestService pageUpdateRestService;

    private ApiProviderRestService apiProvider;

    private ThesaurusUpdateRestService thesaurusUpdateRestService;

    @Getter
    private final String description;

    private final String jwsIssuer;
    private final byte[] jwsKey;
    private final String jwsUser;

    private final ConditionalBasicAuthentication authentication;


    private ClassificationService classificationService;

    @SuppressWarnings({"SpringAutowiredFieldsWarningInspection", "OptionalUsedAsFieldOrParameterType"})
    @Named
    public static class Provider implements jakarta.inject.Provider<PageUpdateApiClient> {

        @Inject
        @Named("npo-pages_publisher.baseUrl")
        String baseUrl;
        @Inject
        @Named("npo-pages_publisher.user")
        String user;
        @Inject
        @Named("npo-pages_publisher.password")
        String password;

        @Inject
        @Named("npo-pages_publisher.jwsIssuer")
        Optional<String> jwsIssuer;
        @Inject
        @Named("npo-pages_publisher.jwsKey")
        Optional<String> jwsKey;
        @Inject
        @Named("npo-pages_publisher.jwsUser")
        Optional<String> jwsUser;

        @Inject
        @Named("npo-pages_publisher.connectionRequestTimeout")
        Optional<String> connectionRequestTimeout;
        @Inject
        @Named("npo-pages_publisher.connectTimeout")
        Optional<String> connectTimeout;
        @Inject
        @Named("npo-pages_publisher.socketTimeout")
        Optional<String> socketTimeout;
        @Inject
        @Named("npo-pages_publisher.maxConnections")
        Optional<Integer> maxConnections;
        @Inject
        @Named("npo-pages_publisher.maxConnectionsPerRoute")
        Optional<Integer> maxConnectionsPerRoute;
        @Inject
        @Named("npo-pages_publisher.warnThreshold")
        Optional<String> warnThreshold;


        @Inject
        @Named("npo-pages_publisher.connectionInPoolTTL")
        Optional<String> connectionInPoolTTL;

        @Inject
        @Named("npo-pages_publisher.validateAfterInactivity")
        Optional<String> validateAfterInactivity;

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
        Duration validateAfterInactivity,
        Duration countWindow,
        Integer bucketCount,
        Duration warnThreshold,
        Level warnLevel,
        List<Locale> acceptableLanguages,
        MediaType accept,
        Boolean trustAll,
        String user,
        String password,
        String mbeanName,
        ClassificationService classificationService,
        String jwsIssuer,
        String jwsKey,
        String jwsUser,
        ClassLoader classLoader,
        String userAgent,
        Boolean registerMBean,
        boolean eager
        ) {
        super(baseUrl == null ? "https://publish.pages.omroep.nl/api" : (baseUrl + (baseUrl.endsWith("/") ?  "" : "/") + "api"),
            connectionRequestTimeout,
            connectTimeout,
            socketTimeout,
            maxConnections,
            maxConnectionsPerRoute,
            null,
            null,
            connectionInPoolTTL,
            validateAfterInactivity,
            countWindow,
            bucketCount,
            warnThreshold,
            warnLevel,
            acceptableLanguages,
            accept,
            null,
            trustAll,
            null,
            mbeanName,
            classLoader,
            userAgent,
            registerMBean,
            eager
        );
        if (user == null){
            throw new IllegalArgumentException("No user given");
        }
        if (password == null) {
            throw new IllegalArgumentException("No  password given");
        }
        authentication = new ConditionalBasicAuthentication(user, password);
        description = user + "@" + this.getBaseUrl();
        this.classificationService = classificationService;
        this.jwsIssuer = jwsIssuer;
        this.jwsKey = jwsKey == null ? null : jwsKey.getBytes();
        this.jwsUser = jwsUser;

    }

    @Override
    protected Stream<Supplier<?>> services() {
        return Stream.of(
            this::getPageUpdateRestService,
            this::getThesaurusUpdateRestService,
            this::getClassificationService,
            this::getProviderRestService
        );
    }

    @Override
    protected void appendTestResult(StringBuilder builder, String arg) {
        super.appendTestResult(builder, arg);
        try {
            String url = MBeans.isBlank(arg) ? "http://www-acc.2doc.nl/documentaires/series/2doc/2016/maart/paradijs-glaswater0.html" : arg;
            PageUpdate load = getPageUpdateRestService().load(url, true, PageIdMatch.BOTH);
            builder.append("load(").append(url).append(") = ").append(load);
        } catch (Exception e) {
            builder.append("Could not load ").append(arg).append(": ").append(e.getMessage());
        }
    }

    public static Builder configured(String... configFiles) {
        Builder builder = builder();
        log.info("Reading configuration from {}", Arrays.asList(configFiles));
        ConfigUtils.configured(builder, configFiles);
        return builder;
    }

    public static Builder configured(Env env, String... configFiles) {
        Builder  builder = builder();
        ConfigUtils.configured(env, builder, configFiles);
        return builder;
    }

    public static Builder configured(Env env, Map<String, String> settings) {
        Builder  builder = builder();
        ConfigUtils.configured(env, builder, settings);
        return builder;
    }


    public static Builder configured(Env env) {
        Builder builder = builder();
        Config config = new Config(URLS_FILE, CONFIG_FILE);
        config.setEnv(env);
        ReflectionUtils.configured(builder, config.getProperties(Config.Prefix.pages_publisher));
        return builder;
    }

    public static Builder configured() {
        return configured((Env) null);
    }


    public PageUpdateRestService getPageUpdateRestService() {
        return pageUpdateRestService = produceIfNull(
            () -> pageUpdateRestService,
            () -> proxyErrorsAndCount(
                PageUpdateRestService.class,
                    getTarget(getClientHttpEngine())
                        .proxyBuilder(PageUpdateRestService.class)
                        .classloader(classLoader)
                        .defaultConsumes(MediaType.APPLICATION_XML)
                        .build(),
                Error.class
            ));
    }


    public ApiProviderRestService getProviderRestService() {
        return apiProvider = produceIfNull(
            () -> apiProvider,
            () -> proxyErrorsAndCount(
                ApiProviderRestService.class,
                    getTarget(getClientHttpEngine())
                        .proxyBuilder(ApiProviderRestService.class)
                        .classloader(classLoader)
                        .defaultConsumes(MediaType.APPLICATION_XML)
                        .build(),
                Error.class
            ));
    }


    public ThesaurusUpdateRestService getThesaurusUpdateRestService() {
        return thesaurusUpdateRestService = produceIfNull(
            () -> thesaurusUpdateRestService,
            () -> proxyErrorsAndCount(ThesaurusUpdateRestService.class,
                proxyForJws(
                    getTarget(getClientHttpEngine())
                        .proxyBuilder(ThesaurusUpdateRestService.class)
                        .classloader(classLoader)
                        .defaultConsumes(MediaType.APPLICATION_XML)
                        .build()
                ),
                Error.class
            ));
    }


    public ThesaurusUpdateRestService proxyForJws(ThesaurusUpdateRestService clean) {
        return (ThesaurusUpdateRestService) Proxy.newProxyInstance(
            classLoader,
            new Class[]{ThesaurusUpdateRestService.class}, new JwsAspect(clean)
        );
    }



    public ClassificationService getClassificationService() {
        return classificationService = produceIfNull(
            () -> classificationService,
            () -> {
                String classificationUrl = this.baseUrl.replaceAll("/api$", "");

                ClassificationService impl = new CachedURLClassificationServiceImpl(classificationUrl);
                log.info("No classification service wired. Created {}", impl);
                return impl;
            });
    }

    @Override
    public String toString() {
        return getClass().getName() + " " + getDescription();
    }

    @Override
    protected void buildResteasy(ResteasyClientBuilder builder) {
        builder
            .register(authentication)
        ;
    }


    @Override
    public synchronized void invalidate() {
        super.invalidate();
        pageUpdateRestService = null;
        thesaurusUpdateRestService = null;
    }

    private Supplier<VersionResult> version = null;

    public String getVersion() {
        if (version == null) {
            version = Suppliers.memoizeWithExpiration(() -> Swagger.getVersionFromSwagger(baseUrl, "5.8"), 30, TimeUnit.MINUTES);
        }
        return version.get().getVersion();
    }

    public boolean isAvailable() {
        return version.get().isAvailable();
    }

    public IntegerVersion getVersionNumber() {
        return Swagger.getVersionNumber(getVersion());
    }


    protected String jws(String subject) {
        Instant now = Instant.now();
        Instant expires = now.plus(Duration.ofHours(12));
        SecretKey secretKey = Keys.hmacShaKeyFor(jwsKey);
        String compactJws = Jwts.builder()
            .subject(subject)
            .claim("usr", jwsUser)
            .issuedAt(Date.from(now))
            .issuer(jwsIssuer)
            .expiration(Date.from(expires))
            .signWith(secretKey)
            .compact();
        log.debug(compactJws);
        return compactJws;
    }

    protected class JwsAspect implements InvocationHandler  {

        private final ThesaurusUpdateRestService proxied;

        public JwsAspect(ThesaurusUpdateRestService proxied) {
            this.proxied = proxied;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (args != null) {

                Annotation[][] annotations = method.getParameterAnnotations();
                for (int i = 0; i < annotations.length; i++) {
                    for (Annotation o : annotations[i]) {
                        if (o instanceof HeaderParam headerParam) {
                            if (headerParam.value().equals(HttpHeaders.AUTHORIZATION)) {
                                args[i] = ThesaurusUpdateRestService.AUTHENTICATION_SCHEME + " " + jws(method.getName());
                            }
                        }
                    }
                }
            }
            return method.invoke(proxied, args);
        }
    }

}
