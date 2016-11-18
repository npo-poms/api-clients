package nl.vpro.api.client.resteasy;


import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.api.rs.v3.page.PageRestService;
import nl.vpro.api.rs.v3.profile.ProfileRestService;
import nl.vpro.api.rs.v3.schedule.ScheduleRestService;
import nl.vpro.api.rs.v3.schedule.ScheduleRestServiceWithDefaults;
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

    private String apiKey;
    private String secret;
    private String origin;

    @Inject
    public NpoApiClients(
        @Named("npo-api.baseUrl") String baseUrl,
        @Named("npo-api.apiKey") String apiKey,
        @Named("npo-api.secret") String secret,
        @Named("npo-api.origin") String origin,
        @Named("npo-api.connectionTimeout") Integer connectionTimeout,
        @Named("npo-api.trustAll") Boolean trustAll
        ) {
		super((baseUrl == null ? "https://rs.poms.omroep.nl/v1" : baseUrl)  + "/api", connectionTimeout, 16, 3);
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
        this(apiBaseUrl, apiKey, secret, origin, 10, false);
    }

    @Builder
    public NpoApiClients(
        String baseUrl,
        Duration connectionRequestTimeout,
        Duration connectTimeout,
        Duration socketTimeout,
        int maxConnections,
        Duration connectionInPoolTTL,
        Duration countWindow,
        List<Locale> acceptableLanguages,
        MediaType mediaType,
        Boolean trustAll,
        String apiKey,
        String secret,
        String origin


    ) {
        super((baseUrl == null ? "https://rs.poms.omroep.nl/v1" : baseUrl) + "/api",
            connectionRequestTimeout, connectTimeout, socketTimeout, maxConnections, connectionInPoolTTL, countWindow, acceptableLanguages, mediaType, trustAll);
        this.apiKey = apiKey;
        this.secret = secret;
        this.origin = origin;


    }


    private static final Pattern VERSION = Pattern.compile(".*?/REL-(.*?)/.*");
    public String getVersion() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonFactory factory = new JsonFactory();
            URL url = new URL(baseUrl + "/swagger.json");
            JsonParser jp = factory.createParser(url.openStream());
            JsonNode swagger = mapper.readTree(jp);
            String versionString = swagger.get("info").get("version").asText();
            Matcher matcher = VERSION.matcher(versionString);
            if (matcher.find()) {
                return matcher.group(1);
            } else {
                return versionString;
            }
        } catch (JsonParseException jpe) {
            log.warn(jpe.getMessage());
            return "4.7.3";
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return "unknown";
        }

    }
    public Float getVersionNumber() {
        Matcher matcher = Pattern.compile("(\\d+\\.\\d+).*").matcher(getVersion());
        matcher.find();
        return Float.parseFloat(matcher.group(1));
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
                buildWithErrorClass(getClientHttpEngine(), MediaRestService.class,  Error.class);
        }
        return mediaRestServiceProxy;
    }

    public MediaRestService getMediaServiceNoTimeout() {
        if (mediaRestServiceProxyNoTimeout == null) {
            mediaRestServiceProxyNoTimeout =
                buildWithErrorClass(getClientHttpEngineNoTimeout(), MediaRestService.class, Error.class);
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
                build(getClientHttpEngine(), PageRestService.class);
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

    @Override
    public void invalidate() {
        super.invalidate();
        mediaRestServiceProxy = null;
        mediaRestServiceProxyNoTimeout = null;
        scheduleRestServiceProxy = null;
        pageRestServiceProxy = null;
        profileRestServiceProxy = null;
    }

    public ApiAuthenticationRequestFilter getAuthentication() {
        return new ApiAuthenticationRequestFilter(apiKey, secret, origin);
    }

	@Override
	public String toString() {
		return super.toString() + " " + baseUrl;
	}


	@Override
    protected void buildResteasy(ResteasyClientBuilder builder) {
        builder.register(getAuthentication());
    }


}
