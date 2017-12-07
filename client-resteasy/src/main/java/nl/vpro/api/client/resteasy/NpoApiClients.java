package nl.vpro.api.client.resteasy;


import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Suppliers;

import nl.vpro.api.client.utils.Config;
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
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.util.ConfigUtils;
import nl.vpro.util.Env;
import nl.vpro.util.ProviderAndBuilder;
import nl.vpro.util.ReflectionUtils;

import static nl.vpro.api.client.utils.Config.CONFIG_FILE;


public class NpoApiClients extends AbstractApiClient  {


    private MediaRestService mediaRestServiceProxy;
    private MediaRestService mediaRestServiceProxyNoTimeout;
    private PageRestService pageRestServiceProxy;
    private ScheduleRestServiceWithDefaults scheduleRestServiceProxy;
    private ProfileRestService profileRestServiceProxy;
    private TVVodRestService tvVodRestServiceProxy;
    private SubtitlesRestService subtitlesRestServiceProxy;
    private ThesaurusRestService thesaurusRestService;

    private String apiKey;
    private String secret;
    private String origin;

    private ThreadLocal<String> properties = ThreadLocal.withInitial(() -> null);
    private ThreadLocal<String> profile = ThreadLocal.withInitial(() -> null);
    private ThreadLocal<Integer> max = ThreadLocal.withInitial(() -> null);


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

        public Builder env(Env env) {
            if(env != null) {
                Map<String, String> defaultProperties = ConfigUtils.filtered(env, Config.Prefix.npo_api.getKey(),
                    ConfigUtils.getPropertiesInHome(CONFIG_FILE)
                );
                ReflectionUtils.configureIfNull(this, defaultProperties);
            }
            return this;
        }

        public NpoApiClients build() {
            log.info("potver");
            return _build();
        }

    }


    @lombok.Builder(builderClassName = "Builder", buildMethodName = "_build")
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
        Env env
    ) {
        super((baseUrl == null ? "https://rs.poms.omroep.nl/v1" : baseUrl) + "/api",
            connectionRequestTimeout,
            connectTimeout,
            socketTimeout,
            maxConnections == null ? 100 : maxConnections,
            maxConnectionsPerRoute == null ? 100 : maxConnectionsPerRoute,
            maxConnectionsNoTimeout,
            maxConnectionsPerRouteNoTimeout,
            connectionInPoolTTL,
            countWindow,
            bucketCount,
            warnThreshold,
            acceptableLanguages,
            accept,
            contentType,
            trustAll,
            objectMapper,
            mbeanName
            );
        this.apiKey = apiKey;
        this.secret = secret;
        this.origin = origin;
        this.properties = ThreadLocal.withInitial(() -> properties);
        this.profile = ThreadLocal.withInitial(() -> profile);
        this.max = ThreadLocal.withInitial(() -> max);


    }

    private static final Pattern VERSION = Pattern.compile(".*?/REL-(.*?)/.*");
    private Supplier<String> version = null;
    public String getVersion() {
        if (version == null) {
            version = Suppliers.memoizeWithExpiration(() -> {
                String result = "unknown";
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonFactory factory = new JsonFactory();
                    URL url = new URL(baseUrl + "/swagger.json");
                    JsonParser jp = factory.createParser(url.openStream());
                    JsonNode swagger = mapper.readTree(jp);
                    String versionString = swagger.get("info").get("version").asText();
                    Matcher matcher = VERSION.matcher(versionString);
                    if (matcher.find()) {
                        result = matcher.group(1);
                    } else {
                        result = "5.5";
                        log.info("No version found in " + versionString + " supposing " + result);
                    }
                } catch (JsonParseException jpe) {
                    log.warn(jpe.getMessage());
                    result = "4.7.3";
                } catch (ConnectException e) {
                    log.warn(e.getMessage());
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
                return result;
            }, 30, TimeUnit.MINUTES);
        }
        return version.get();
    }

    /**
     * The version of the npo frontend api we are talking too.
     * @return a float representing the major/minor version. The patch level is added as thousands.
     */
    public Float getVersionNumber() {
        Matcher matcher = Pattern.compile("(\\d+\\.\\d+)\\.?(\\d+)?.*").matcher(getVersion());
        if (matcher.find()) {
            Double result = Double.parseDouble(matcher.group(1));
            String minor = matcher.group(2);
            if (minor != null) {
                result += (double) Integer.parseInt(minor) / 1000d;
            }
            return result.floatValue();
        } else {
            log.info("Version {} could not be parsed", getVersion());
            return 0f;
        }
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
        return properties.get();
    }

    public void setProperties(String properties) {
        this.properties.set(properties);
    }
    public boolean hasAllProperties() {
        String p = properties.get();
        return p == null || p .equals("all");
    }

    public String getProfile() {
        return profile.get();
    }

    public void setProfile(String profile) {
        this.profile.set(profile);
    }

    public Integer getMax() {
        return max.get();
    }

    public void setMax(Integer max) {
        this.max.set(max);
    }

    public static NpoApiClients.Builder configured(String... configFiles)  {
        NpoApiClients.Builder builder = builder();
        ConfigUtils.configured(builder, configFiles);
        return builder;
    }

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

    public static NpoApiClients.Builder configured() {
        return configured((Env) null);
    }

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
            () -> buildWithErrorClass(getClientHttpEngine(), ScheduleRestServiceWithDefaults.class, ScheduleRestService.class, Error.class));

    }

    public PageRestService getPageService() {
        return pageRestServiceProxy = produceIfNull(
            () -> pageRestServiceProxy,
            () -> wrapClientAspect(
                build(getClientHttpEngine(), PageRestService.class),
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
    public void invalidate() {
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
        return getApiKey() + "@" + baseUrl;
    }

    @Override
    protected void buildResteasy(ResteasyClientBuilder builder) {
        builder.register(getAuthentication())
            .register(VTTSubtitlesReader.class)
            .register(NpoApiClientsFilter.class)
        ;
    }
}
