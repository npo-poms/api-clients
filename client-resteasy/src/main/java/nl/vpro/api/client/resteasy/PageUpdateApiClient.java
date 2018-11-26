package nl.vpro.api.client.resteasy;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import com.google.common.base.Suppliers;

import nl.vpro.api.client.utils.Config;
import nl.vpro.api.client.utils.Swagger;
import nl.vpro.api.client.utils.VersionResult;
import nl.vpro.domain.classification.CachedURLClassificationServiceImpl;
import nl.vpro.domain.classification.ClassificationService;
import nl.vpro.domain.page.update.PageUpdate;
import nl.vpro.rs.pages.update.PageUpdateRestService;
import nl.vpro.rs.provider.ApiProviderRestService;
import nl.vpro.rs.thesaurus.update.ThesaurusUpdateRestService;
import nl.vpro.util.ConfigUtils;
import nl.vpro.util.Env;
import nl.vpro.util.ProviderAndBuilder;
import nl.vpro.util.ReflectionUtils;

import static nl.vpro.api.client.utils.Config.CONFIG_FILE;

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


    @Override
    protected void appendTestResult(StringBuilder builder, String arg) {
        super.appendTestResult(builder, arg);
        builder.append(getPageUpdateRestService());
        builder.append("\n");
        try {
            PageUpdate load = getPageUpdateRestService().load(arg);
            builder.append("load(").append(arg).append(")").append(load);
        } catch (Exception e) {
            builder.append("Could not load ").append(arg).append(": ").append(e.getMessage());
        }
        builder.append("\n");
        builder.append("version:").append(getVersion()).append("\n");
        builder.append(getThesaurusUpdateRestService());
        builder.append("\n");
        builder.append(getClassificationService());
        builder.append("\n");
    }

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
        @Named("npo-pageupdate-api.jwsIssuer")
        Optional<String> jwsIssuer;
        @Inject
        @Named("npo-pageupdate-api.jwsKey")
        Optional<String> jwsKey;
        @Inject
        @Named("npo-pageupdate-api.jwsUser")
        Optional<String> jwsUser;

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
        String userAgent
        ) {
        super(
            baseUrl == null ? "https://publish.pages.omroep.nl/api" : (baseUrl + (baseUrl.endsWith("/") ?  "" : "/") + "api"),
            connectionRequestTimeout,
            connectTimeout,
            socketTimeout,
            maxConnections,
            maxConnectionsPerRoute,
            null,
            null,
            connectionInPoolTTL,
            countWindow,
            bucketCount,
            warnThreshold,
            acceptableLanguages,
            accept,
            null,
            trustAll,
            null,
            mbeanName,
            classLoader,
            userAgent
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
        Config config = new Config(CONFIG_FILE);
        config.setEnv(env);
        ReflectionUtils.configured(builder, config.getProperties(Config.Prefix.npo_pageupdate_api));
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

                log.info("No classification service wired. Created {}", this.classificationService);
                return new CachedURLClassificationServiceImpl(this.baseUrl);
            });
    }

    @Override
    public String toString() {
        return getClass().getName() + " " + getDescription();
    }

    @Override
    protected void buildResteasy(ResteasyClientBuilder builder) {
        builder
            .register(authentication);
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

    public Float getVersionNumber() {
        return Swagger.getVersionNumber(getVersion());
    }


    protected String jws() {
        Instant now = Instant.now();
        Instant expires = now.plus(Duration.ofHours(12));
        String compactJws = Jwts.builder()
            .setSubject("GTAAPerson")
            .claim("usr", jwsUser)
            .setIssuedAt(Date.from(now))
            .setIssuer(jwsIssuer)
            .setExpiration(Date.from(expires))
            .signWith(SignatureAlgorithm.HS256, jwsKey)
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
                        if (o instanceof HeaderParam) {
                            HeaderParam headerParam = (HeaderParam) o;
                            if (headerParam.value().equals(HttpHeaders.AUTHORIZATION)) {
                                args[i] = ThesaurusUpdateRestService.AUTHENTICATION_SCHEME + " " + jws();
                            }
                        }
                    }
                }
            }
            return method.invoke(proxied, args);
        }


    }

    public class ConditionalBasicAuthentication extends BasicAuthentication {

        public ConditionalBasicAuthentication(String username, String password) {
            super(username, password);
        }

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            List<Object> existing = requestContext.getHeaders().get(HttpHeaders.AUTHORIZATION);
            if (existing == null || existing.isEmpty()) {
                super.filter(requestContext);
            } else {
                log.debug("Request already contains other authorization headers {}", existing);
            }
        }
    }


}
