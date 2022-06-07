package nl.vpro.api.client.frontend;


import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.meeuw.functional.TriFunction;
import org.slf4j.event.Level;

import com.google.common.base.Suppliers;

import nl.vpro.api.client.resteasy.AbstractApiClient;
import nl.vpro.api.client.utils.Config;
import nl.vpro.api.client.utils.Swagger;
import nl.vpro.api.rs.subtitles.VTTSubtitlesReader;
import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.api.rs.v3.page.PageRestService;
import nl.vpro.api.rs.v3.profile.ProfileRestService;
import nl.vpro.api.rs.v3.schedule.ScheduleRestService;
import nl.vpro.api.rs.v3.schedule.ScheduleRestServiceWithDefaults;
import nl.vpro.api.rs.v3.subtitles.SubtitlesRestService;
import nl.vpro.api.rs.v3.thesaurus.ThesaurusRestService;
import nl.vpro.api.rs.v3.tvvod.TVVodRestService;
import nl.vpro.domain.api.Error;
import nl.vpro.domain.api.profile.Profile;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.jmx.Description;
import nl.vpro.poms.shared.Headers;
import nl.vpro.rs.client.VersionResult;
import nl.vpro.util.*;

import static nl.vpro.api.client.utils.Config.CONFIG_FILE;


/**
 * Collects clients for all services on the <a href="https://rs.poms.omroep.nl/v1">NPO Frontend Rest API</a>
 *
 * This is implemented by proxying the actual service interfaces used on the server. Most noticably {@link MediaRestService} and {@link PageRestService}
 */
@Description("Api clients for services on https://rs.poms.omroep.nl")
public class NpoApiClients extends AbstractApiClient {

    public static TriFunction<Method, Object[], String, Level> DEFAULT_HEADER_LEVEL = (m, a, s) ->
        s.equalsIgnoreCase(Headers.NPO_WARNING_HEADER) ? Level.WARN : Level.DEBUG;

    private MediaRestService mediaRestServiceProxy;
    private MediaRestService mediaRestServiceProxyNoTimeout;
    private PageRestService pageRestServiceProxy;
    private PageRestService pageRestServiceProxyNoTimeout;
    private ScheduleRestServiceWithDefaults scheduleRestServiceProxy;
    private ProfileRestService profileRestServiceProxy;
    private TVVodRestService tvVodRestServiceProxy;
    private SubtitlesRestService subtitlesRestServiceProxy;
    private ThesaurusRestService thesaurusRestService;

    private String apiKey;
    private String secret;
    private String origin;

    private final ThreadLocal<String> propertiesThreadLocal;
    protected String properties;
    private final ThreadLocal<String> profileThreadLocal;
    protected String profile;
    private final ThreadLocal<Integer> maxThreadLocal;
    protected Integer max;

    protected Function<NpoApiClients, String> toString;

    @Getter
    private final TriFunction<Method, Object[], String, Level> headerLevel;


    @SuppressWarnings({"SpringAutowiredFieldsWarningInspection", "unused", "OptionalUsedAsFieldOrParameterType"})
    @Named
    public static class Provider implements javax.inject.Provider<NpoApiClients> {
        @Inject
        @Named("npo-api.baseUrl")
        String baseUrl;
        @Inject
        @Named("npo-api.apiKey")
        String apiKey;
        @Inject
        @Named("npo-api.secret")
        String secret;
        @Inject
        @Named("npo-api.origin")
        String origin;

        @Inject
        @Named("npo-api.connectionRequestTimeout")
        Optional<String> connectionRequestTimeout;
        @Inject
        @Named("npo-api.connectTimeout")
        Optional<String> connectTimeout;
        @Inject
        @Named("npo-api.socketTimeout")
        Optional<String> socketTimeout;
        @Inject
        @Named("npo-api.maxConnections")
        Optional<Integer> maxConnections;
        @Inject
        @Named("npo-api.maxConnectionsPerRoute")
        Optional<Integer> maxConnectionsPerRoute;

        @Inject
        @Named("npo-api.maxConnectionsNoTimeout")
        Optional<Integer> maxConnectionsNoTimeout;

        @Inject
        @Named("npo-api.maxConnectionsPerRouteNoTimeout")
        Optional<Integer> maxConnectionsPerRouteNoTimeout;


        @Inject
        @Named("npo-api.connectionInPoolTTL")
        Optional<String> connectionInPoolTTL;

        @Inject
        @Named("npo-api.validateAfterInactivity")
        Optional<String> validateAfterInactivity;

        @Inject
        @Named("npo-api.trustAll")
        Optional<Boolean> trustAll;

        @Inject
        @Named("npo-api.warnThreshold")
        Optional<String> warnThreshold;

        @Inject
        @Named("npo-api.countWindow")
        Optional<String> countWindow;

        @Inject
        @Named("npo-api.bucketCount")
        Optional<Integer> bucketCount;

        private final ClassLoader classLoader = NpoApiClients.class.getClassLoader();

        public Builder builder = builder();

        public Builder env(Env env) {
            builder.env(env);
            return builder;
        }

        @Override
        public NpoApiClients get() {
            return ProviderAndBuilder.fillAndCatch(this, builder).build();
        }
    }


    @Named
    @Slf4j
    public static class Builder {
        private Env env = null;
        public Builder env(Env env) {
            this.env = env;
            return this;
        }

        public NpoApiClients build() {
            if (env != null) {
                Map<String, String> defaultProperties = ConfigUtils.filtered(env, Config.Prefix.npo_api.getKey(),
                    ConfigUtils.getPropertiesInHome(CONFIG_FILE)
                );
                ReflectionUtils.configureIfNull(this, defaultProperties, Collections.emptyList(), Collections.emptyList());
            }
            NpoApiClients result =  _build();
            result.postConstruct();
            return result;
        }

    }


    @lombok.Builder(builderClassName = "Builder", buildMethodName = "_build", toBuilder = true)
    protected NpoApiClients(
        String baseUrl,
        Duration connectionRequestTimeout,
        Duration connectTimeout,
        Duration socketTimeout,
        Integer maxConnections,
        Integer maxConnectionsPerRoute,
        Integer maxConnectionsNoTimeout,
        Integer maxConnectionsPerRouteNoTimeout,
        Duration connectionInPoolTTL,
        Duration validateAfterInactivity,
        Duration countWindow,
        Integer bucketCount,
        Duration warnThreshold,
        @Singular List<Locale> acceptableLanguages,
        MediaType accept,
        MediaType contentType,
        Boolean trustAll,
        String apiKey,
        String secret,
        String origin,
        String properties,
        String profile,
        Integer max,
        Jackson2Mapper objectMapper,
        String mbeanName,
        ClassLoader classLoader,
        String userAgent,
        Boolean registerMBean,
        Function<NpoApiClients, String> toString,
        TriFunction<Method, Object[], String, Level> headerLevel,
        boolean eager
    ) {
        super(withApiPostFix(baseUrl == null ? "https://rs.poms.omroep.nl/v1" : baseUrl),
            connectionRequestTimeout,
            connectTimeout,
            socketTimeout,
            maxConnections == null ? 100 : maxConnections,
            maxConnectionsPerRoute == null ? 100 : maxConnectionsPerRoute,
            maxConnectionsNoTimeout,
            maxConnectionsPerRouteNoTimeout,
            connectionInPoolTTL,
            validateAfterInactivity,
            countWindow,
            bucketCount,
            warnThreshold,
            acceptableLanguages,
            accept,
            contentType,
            trustAll,
            objectMapper,
            mbeanName,
            classLoader,
            userAgent,
            registerMBean,
            eager
            );
        this.apiKey = apiKey;

        this.secret = secret;
        this.origin = origin;
        this.properties = properties;
        this.propertiesThreadLocal = ThreadLocal.withInitial(() -> this.properties);
        this.profile = profile;
        this.profileThreadLocal = ThreadLocal.withInitial(() -> this.profile);
        this.max = max;
        this.maxThreadLocal = ThreadLocal.withInitial(() -> this.max);
        if (this.apiKey == null) {
            log.warn("No api key configured for {}",  this);
        }
        if (this.secret == null) {
            log.warn("No api secret configured for {}", this);
        }
        if (this.origin == null) {
            log.warn("No api origin configured for {}", this);
        }
        this.toString = toString;
        this.headerLevel = headerLevel == null ? DEFAULT_HEADER_LEVEL : headerLevel;
    }

    @Override
    protected Stream<Supplier<?>> services() {
        return Stream.of(
            this::getMediaService,
            this::getPageService,
            this::getProfileService,
            this::getScheduleService,
            this::getSubtitlesRestService,
            this::getThesaurusRestService,
            this::getTVVodService
        );
    }

    @Override
    protected void appendTestResult(StringBuilder builder, String arg) {
        super.appendTestResult(builder, arg);
        try {
            MediaObject load = getMediaService().load(arg, null, null);
            builder.append("load(").append(arg).append(")").append(load);
        } catch (Exception e) {
           builder.append("Could not load ").append(arg).append(": ").append(e.getMessage());
        }
        builder.append("version:").append(getVersion());

    }

    private static String withApiPostFix(String baseUrl) {
       return baseUrl.endsWith("/api") ? baseUrl : baseUrl + "/api";
    }

    private Supplier<VersionResult> version = null;
    public String getVersion() {
        if (version == null) {
            version = Suppliers.memoizeWithExpiration(() -> Swagger.getVersionFromSwagger(baseUrl, "5.23"), 30, TimeUnit.MINUTES);
        }
        return version.get().getVersion();
    }

    public boolean isAvailable() {
        getVersion();
        return version.get().isAvailable();
    }

    /**
     * The version of the npo frontend api we are talking too.
     * @return a float representing the major/minor version. The patch level is added as thousands.
     */
    public IntegerVersion getVersionNumber() {
        return Swagger.getVersionNumber(getVersion());
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        this.invalidate();
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
        this.invalidate();
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
        this.invalidate();
    }

    public String getProperties() {
        return propertiesThreadLocal.get();
    }

    public void setProperties(String properties) {
        this.propertiesThreadLocal.set(properties);
    }
    public boolean hasAllProperties() {
        String p = propertiesThreadLocal.get();
        return p == null || p .equals("all");
    }

    public String getProfile() {
        return profileThreadLocal.get();
    }
    public Optional<Profile> getAssociatedProfile() {
        return Optional.ofNullable(profileThreadLocal.get()).map(p -> getProfileService().load(p, null));
    }

    public void setProfile(String profile) {
        this.profileThreadLocal.set(profile);
    }

    public Integer getMax() {
        return maxThreadLocal.get();
    }

    public void setMax(Integer max) {
        this.maxThreadLocal.set(max);
    }

    /**
     * Creates a builder (see {@link #builder()}, and configures it the the given config file (using {@link ConfigUtils#configured(Object, String...)}
     */
    public static NpoApiClients.Builder configured(String... configFiles)  {
        NpoApiClients.Builder builder = builder();
        ConfigUtils.configured(builder, configFiles);
        return builder;
    }

    /**
     * Creates a builder (see {@link #builder()}, and configures it the the given config file (using {@link ConfigUtils#configured(Object, String...)}
     */
    public static NpoApiClients.Builder configured(Env env, String... configFiles) {
        NpoApiClients.Builder builder = builder();
        ConfigUtils.configured(env, builder, configFiles);
        return builder;
    }

    public static NpoApiClients.Builder configured(Map<String, String> settings) {
        NpoApiClients.Builder builder = builder();
        ReflectionUtils.configured(builder, settings);
        return builder;
    }

    public static NpoApiClients.Builder configured(Env env, Map<String, String> settings) {
        NpoApiClients.Builder builder = builder();
        ConfigUtils.configured(env, builder, settings);
        return builder;
    }

    /**
     * Creates {@link Builder}, which is configured using the defaults on the class path, the overrides in {@code ${USER.HOME}/conf/}{@link Config#CONFIG_FILE}, and perhaps the 'env' system setting.
     */
    public static NpoApiClients.Builder configured() {
        return configured((Env) null);
    }

   /**
     * Creates {@link nl.vpro.api.client.frontend.NpoApiClients.Builder}, which is configured using the defaults on the class path, the overrides in {@code ${USER.HOME}/conf/}{@link Config#CONFIG_FILE}, but sets the environment explicitely
    */
   public static NpoApiClients.Builder configured(Env env) {
       NpoApiClients.Builder builder = builder();
       Config config = new Config(CONFIG_FILE);
       config.setEnv(env);
       ReflectionUtils.configured(builder, config.getProperties(Config.Prefix.npo_api));
       return builder;
   }

    public MediaRestService getMediaService() {
        return mediaRestServiceProxy = produceIfNull(
            () -> mediaRestServiceProxy,
            () -> wrapClientAspect(
                buildWithErrorClass(getClientHttpEngine(), MediaRestService.class, Error.class),
                MediaRestService.class));
    }

    public MediaRestService getMediaServiceNoTimeout() {
        return mediaRestServiceProxyNoTimeout = produceIfNull(
            () -> mediaRestServiceProxyNoTimeout,
            () -> wrapClientAspect(
                buildWithErrorClass(getClientHttpEngineNoTimeout(), MediaRestService.class, Error.class),
                MediaRestService.class));
    }

    public ScheduleRestServiceWithDefaults getScheduleService() {
        return scheduleRestServiceProxy = produceIfNull(
            () -> scheduleRestServiceProxy,
            () -> wrapClientAspect(
                buildWithErrorClass(getClientHttpEngine(), ScheduleRestServiceWithDefaults.class, ScheduleRestService.class, Error.class),
                ScheduleRestServiceWithDefaults.class
            )
        );

    }

    public PageRestService getPageService() {
        return pageRestServiceProxy = produceIfNull(
            () -> pageRestServiceProxy,
            () -> wrapClientAspect(
                build(getClientHttpEngine(), PageRestService.class),
                PageRestService.class));
    }


    public PageRestService getPageServiceNoTimeout() {
        return pageRestServiceProxyNoTimeout = produceIfNull(
            () -> pageRestServiceProxyNoTimeout,
            () -> wrapClientAspect(
                build(getClientHttpEngineNoTimeout(), PageRestService.class),
                PageRestService.class));
    }

    public ProfileRestService getProfileService() {
        return profileRestServiceProxy = produceIfNull(
            () -> profileRestServiceProxy,
            () -> build(getClientHttpEngine(), ProfileRestService.class));
    }

    public TVVodRestService getTVVodService() {
        return tvVodRestServiceProxy = produceIfNull(
            () -> tvVodRestServiceProxy,
            () -> build(getClientHttpEngine(), TVVodRestService.class));
    }

    public SubtitlesRestService getSubtitlesRestService() {
        return subtitlesRestServiceProxy = produceIfNull(
            () -> subtitlesRestServiceProxy,
            () -> build(getClientHttpEngine(), SubtitlesRestService.class));
    }

    public ThesaurusRestService getThesaurusRestService() {
        return thesaurusRestService = produceIfNull(
            () -> thesaurusRestService,
            () -> build(getClientHttpEngine(), ThesaurusRestService.class));
    }


    protected <T> T wrapClientAspect(T proxy, Class<T> service) {
        return NpoApiClientsAspect.proxy(this, proxy, service);
    }

    @Override
    public synchronized void invalidate() {
        super.invalidate();
        mediaRestServiceProxy = null;
        mediaRestServiceProxyNoTimeout = null;
        scheduleRestServiceProxy = null;
        pageRestServiceProxy = null;
        profileRestServiceProxy = null;
        version = null;
    }

    public ApiAuthenticationRequestFilter getAuthentication() {
        return new ApiAuthenticationRequestFilter(apiKey, secret, origin);
    }

    @Override
    public String toString() {
        if (toString == null) {
            return getApiKey() + "@" + baseUrl;
        } else {
            return toString.apply(this);
        }
    }

    @Override
    protected void buildResteasy(ResteasyClientBuilder builder) {
        builder.register(getAuthentication())
            .register(VTTSubtitlesReader.class)
            .register(NpoApiClientsFilter.class)
        ;
    }

    @Override
    public synchronized void close() {
        super.close();
        propertiesThreadLocal.remove();
        profileThreadLocal.remove();
        maxThreadLocal.remove();
    }
}
