package nl.vpro.api.client.resteasy;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import nl.vpro.domain.classification.CachedURLClassificationServiceImpl;
import nl.vpro.domain.classification.ClassificationService;
import nl.vpro.rs.pages.update.PageUpdateRestService;
import nl.vpro.rs.thesaurus.update.NewPersonRequest;
import nl.vpro.rs.thesaurus.update.ThesaurusUpdateRestService;
import nl.vpro.util.Env;
import nl.vpro.util.ProviderAndBuilder;
import nl.vpro.util.ReflectionUtils;

@Slf4j
public class PageUpdateApiClient extends AbstractApiClient {


    private PageUpdateRestService pageUpdateRestService;

    private ThesaurusUpdateRestService thesaurusUpdateRestService;

    @Getter
    private final String description;

    private final String jwsIssuer;
    private final byte[] jwsKey;
    private final String jwsUser;


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
        @Named("npo-pageupdate-api.jwsIssuer")
        String jwsIssuer;
        @Inject
        @Named("npo-pageupdate-api.jwsKey")
        String jwsKey;
        @Inject
        @Named("npo-pageupdate-api.jwsUser")
        String jwsUser;

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
        ClassificationService classificationService,
        String jwsIssuer,
        String jwsKey,
        String jwsUser
        ) {
        super(baseUrl + (baseUrl.endsWith("/") ?  "" : "/") + "api",
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
        this.jwsIssuer = jwsIssuer;
        this.jwsKey = jwsKey.getBytes();
        this.jwsUser = jwsUser;
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


    public ThesaurusUpdateRestService getThesaurusUpdateRestService() {
        return thesaurusUpdateRestService = produceIfNull(
            () -> thesaurusUpdateRestService,
            () -> proxyErrorsAndCount(ThesaurusUpdateRestService.class,
                proxyForJws(getTarget(getClientHttpEngine())
                    .proxyBuilder(ThesaurusUpdateRestService.class)
                    .defaultConsumes(MediaType.APPLICATION_XML).build()),
                Error.class
            ));
    }


    public ThesaurusUpdateRestService proxyForJws(ThesaurusUpdateRestService clean) {
        return (ThesaurusUpdateRestService) Proxy.newProxyInstance(ThesaurusUpdateRestService.class.getClassLoader(), new Class[]{ThesaurusUpdateRestService.class}, new JwsAspect(clean));
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
            if (args[0] instanceof NewPersonRequest) {
                NewPersonRequest newPerson = (NewPersonRequest) args[0];
                if (newPerson.getJws() == null) {
                    newPerson.setJws(jws());
                }
            }
            return method.invoke(proxied, args);
        }


    }


}
