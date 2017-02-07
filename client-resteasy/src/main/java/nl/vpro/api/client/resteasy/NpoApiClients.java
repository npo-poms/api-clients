package nl.vpro.api.client.resteasy;


import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.api.rs.v3.page.PageRestService;
import nl.vpro.api.rs.v3.profile.ProfileRestService;
import nl.vpro.api.rs.v3.schedule.ScheduleRestService;
import nl.vpro.api.rs.v3.schedule.ScheduleRestServiceWithDefaults;
import nl.vpro.api.rs.v3.subtitles.SubtitlesRestService;
import nl.vpro.api.rs.v3.tvvod.TVVodRestService;
import nl.vpro.domain.api.Error;
import nl.vpro.util.Env;
import nl.vpro.util.ReflectionUtils;

@Named
@Slf4j
public class NpoApiClients extends AbstractApiClient  {

    private MediaRestService mediaRestServiceProxy;
    private MediaRestService mediaRestServiceProxyNoTimeout;
    private PageRestService pageRestServiceProxy;
    private ScheduleRestServiceWithDefaults scheduleRestServiceProxy;
    private ProfileRestService profileRestServiceProxy;
    private TVVodRestService tvVodRestServiceProxy;
    private SubtitlesRestService subtitlesRestServiceProxy;


    private String apiKey;
    private String secret;
    private String origin;

    private ThreadLocal<String> properties = ThreadLocal.withInitial(() -> null);
    private ThreadLocal<String> profile = ThreadLocal.withInitial(() -> null);
    private ThreadLocal<Integer> max = ThreadLocal.withInitial(() -> null);

    @Inject
    public NpoApiClients(
        @Named("npo-api.baseUrl") String baseUrl,
        @Named("npo-api.apiKey") String apiKey,
        @Named("npo-api.secret") String secret,
        @Named("npo-api.origin") String origin,
        @Named("npo-api.connectionTimeout") Integer connectionTimeout, // used for socket, connection and connection request timeouts
        @Named("npo-api.maxConnections") Integer maxConnections,
        @Named("npo-api.maxConnectionsPerRoute") Integer maxConnectionsPerRoute,
        @Named("npo-api.trustAll") Boolean trustAll
        ) {
		super((baseUrl == null ? "https://rs.poms.omroep.nl/v1" : baseUrl)  + "/api",
            connectionTimeout,
            maxConnections,
            maxConnectionsPerRoute, 3);
        this.apiKey = apiKey;
        this.secret = secret;
        this.origin = origin;
        if (trustAll != null) {
            super.setTrustAll(trustAll);
        }
    }

    public NpoApiClients(
        String apiBaseUrl,
        String apiKey,
        String secret,
        String origin
    ) {
        this(apiBaseUrl, apiKey, secret, origin, 10, 20, 2, false);
    }

    @Builder
    public NpoApiClients(
        String baseUrl,
        Duration connectionRequestTimeout,
        Duration connectTimeout,
        Duration socketTimeout,
        int maxConnections,
        int maxConnectionsPerRoute,
        Duration connectionInPoolTTL,
        Duration countWindow,
        List<Locale> acceptableLanguages,
        MediaType mediaType,
        Boolean trustAll,
        String apiKey,
        String secret,
        String origin,
        String properties,
        String profile,
        Integer max
    ) {
        super((baseUrl == null ? "https://rs.poms.omroep.nl/v1" : baseUrl) + "/api",
            connectionRequestTimeout, connectTimeout, socketTimeout, maxConnections, maxConnectionsPerRoute, connectionInPoolTTL, countWindow, acceptableLanguages, mediaType, trustAll);
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
                        result = versionString;
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
        matcher.find();
        Double result = Double.parseDouble(matcher.group(1));
        String minor = matcher.group(2);
        if (minor != null) {
            result += (double) Integer.parseInt(minor) / 1000d;
        }
        return result.floatValue();
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

    public static NpoApiClientsBuilder configured(String... configFiles)  {
        NpoApiClientsBuilder builder = builder();
        ReflectionUtils.configured(builder, configFiles);
        return builder;
    }

    public static NpoApiClientsBuilder configured(Env env, String... configFiles) {
        NpoApiClientsBuilder builder = builder();
        ReflectionUtils.configured(env, builder, configFiles);
        return builder;
    }

    public static NpoApiClientsBuilder configured(Map<String, String> settings) {
        NpoApiClientsBuilder builder = builder();
        ReflectionUtils.configured(builder, settings);
        return builder;
    }

    public static NpoApiClientsBuilder configured(Env env, Map<String, String> settings) {
        NpoApiClientsBuilder builder = builder();
        ReflectionUtils.configured(env, builder, settings);
        return builder;
    }

    public static NpoApiClientsBuilder configured() {
        return configured((Env) null);
    }

    public static NpoApiClientsBuilder configured(Env env) {
        NpoApiClientsBuilder builder = builder();
        ReflectionUtils.configuredInHome(env, builder, "apiclient.properties");
        return builder;
    }

    public MediaRestService getMediaService() {
        if (mediaRestServiceProxy == null) {
            mediaRestServiceProxy =
                wrapClientAspect(
                    buildWithErrorClass(getClientHttpEngine(), MediaRestService.class,  Error.class),
                    MediaRestService.class);
        }
        return mediaRestServiceProxy;
    }

    public MediaRestService getMediaServiceNoTimeout() {
        if (mediaRestServiceProxyNoTimeout == null) {
            mediaRestServiceProxyNoTimeout =
                wrapClientAspect(
                    buildWithErrorClass(getClientHttpEngineNoTimeout(), MediaRestService.class, Error.class),
                    MediaRestService.class);
        }
        return mediaRestServiceProxyNoTimeout;
    }

    public ScheduleRestServiceWithDefaults getScheduleService() {
        if (scheduleRestServiceProxy == null) {
            scheduleRestServiceProxy =
                buildWithErrorClass(getClientHttpEngine(), ScheduleRestServiceWithDefaults.class, ScheduleRestService.class, Error.class);
        }
        return scheduleRestServiceProxy;
    }

    public PageRestService getPageService() {
        if (pageRestServiceProxy == null) {
            pageRestServiceProxy =
                wrapClientAspect(
                    build(getClientHttpEngine(), PageRestService.class),
                    PageRestService.class
                );
        }
        return pageRestServiceProxy;
    }

    public ProfileRestService getProfileService() {
        if (profileRestServiceProxy == null) {
            profileRestServiceProxy =
                build(getClientHttpEngine(), ProfileRestService.class);
        }
        return profileRestServiceProxy;
    }

    public TVVodRestService getTVVodService() {
        if (tvVodRestServiceProxy== null) {
            tvVodRestServiceProxy =
                build(getClientHttpEngine(), TVVodRestService.class);
        }
        return tvVodRestServiceProxy;
    }

    public SubtitlesRestService getSubtitlesRestService() {
        if (subtitlesRestServiceProxy == null) {
            subtitlesRestServiceProxy =
                build(getClientHttpEngine(), SubtitlesRestService.class);
        }
        return subtitlesRestServiceProxy;
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
        builder.register(getAuthentication());
    }
}
