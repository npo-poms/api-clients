package nl.vpro.api.client.resteasy;


import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.api.rs.v3.page.PageRestService;
import nl.vpro.api.rs.v3.profile.ProfileRestService;
import nl.vpro.api.rs.v3.schedule.ScheduleRestService;
import nl.vpro.api.rs.v3.schedule.ScheduleRestServiceWithDefaults;
import nl.vpro.resteasy.JacksonContextResolver;
import nl.vpro.util.ReflectionUtils;

@Named
public class NpoApiClients extends AbstractApiClient implements  NpoApiClientsMBean {

    private static final Logger LOG = LoggerFactory.getLogger(NpoApiClients.class);

    private final ApiAuthenticationRequestFilter authentication;

    private MediaRestService mediaRestServiceProxy;
    private MediaRestService mediaRestServiceProxyNoTimeout;
    private PageRestService pageRestServiceProxy;
    private ScheduleRestServiceWithDefaults scheduleRestServiceProxy;
    private ProfileRestService profileRestServiceProxy;



    @Inject
    public NpoApiClients(
        @Named("npo-api.baseUrl") String apiBaseUrl,
        @Named("npo-api.apiKey") String apiKey,
        @Named("npo-api.secret") String secret,
        @Named("npo-api.origin") String origin,
        @Named("npo-api.connectionTimeout") Integer connectionTimeout,
        @Named("npo-api.trustAll") Boolean trustAll
        ) {
		super(apiBaseUrl + "api", connectionTimeout, 16, 3);
        this.authentication = new ApiAuthenticationRequestFilter(apiKey, secret, origin);
        super.setTrustAll(trustAll);
    }


    public NpoApiClients(
        String apiBaseUrl,
        String apiKey,
        String secret,
        String origin
    ) {
        this(apiBaseUrl, apiKey, secret, origin, 10, false);
    }


    public static Builder configured(String... configFiles)  {
        Builder builder = new Builder();
        ReflectionUtils.configured(builder, configFiles);
        return builder;
    }

    public static Builder configured() {
        return configured(System.getProperty("user.home") + File.separator + "conf" + File.separator + "apiclient.properties");
    }


    public MediaRestService getMediaService() {
        if (mediaRestServiceProxy == null) {
            mediaRestServiceProxy =
                build(getClientHttpEngine(), MediaRestService.class);
        }
        return mediaRestServiceProxy;
    }

    public MediaRestService getMediaServiceNoTimeout() {
        if (mediaRestServiceProxyNoTimeout == null) {
            mediaRestServiceProxyNoTimeout =
                build(getClientHttpEngineNoTimeout(), MediaRestService.class);
        }
        return mediaRestServiceProxyNoTimeout;
    }

    public ScheduleRestServiceWithDefaults getScheduleService() {
        if (scheduleRestServiceProxy == null) {
            scheduleRestServiceProxy =
                build(getClientHttpEngine(), ScheduleRestServiceWithDefaults.class, ScheduleRestService.class);
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
    protected void invalidate() {
        super.invalidate();
        mediaRestServiceProxy = null;
        mediaRestServiceProxyNoTimeout = null;
        scheduleRestServiceProxy = null;
        pageRestServiceProxy = null;
        profileRestServiceProxy = null;
    }

    public ApiAuthenticationRequestFilter getAuthentication() {
        return authentication;
    }

	@Override
	public String toString() {
		return super.toString() + " " + baseUrl;
	}


	@Override
    protected ResteasyWebTarget getTarget(ClientHttpEngine engine) {
        ResteasyClient client =
            new ResteasyClientBuilder()
                .httpEngine(engine)
                .register(authentication)
                .register(JacksonContextResolver.class)
                .build();
        return client.target(baseUrl);
    }

    public static class Builder {

        private String apiBaseUrl = "https://rs.poms.omroep.nl/v1/";
        private String apiKey;
        private String secret;
        private String origin;
        private Integer connectionTimeout = 10000;
        private Integer timeOut = 10000;
        private boolean trustAll = false;

        public NpoApiClients build() {
            return new NpoApiClients(apiBaseUrl, apiKey, secret, origin, connectionTimeout, trustAll);
        }

        public String getApiBaseUrl() {
            return apiBaseUrl;
        }

        public Builder setApiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
            return this;
        }


        public String getApiKey() {
            return apiKey;
        }

        public Builder setApiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public String getSecret() {
            return secret;
        }

        public Builder setSecret(String secret) {
            this.secret = secret;
            return this;
        }

        public String getOrigin() {
            return origin;
        }

        public Builder setOrigin(String origin) {
            this.origin = origin;
            return this;
        }

        public Integer getConnectionTimeout() {
            return connectionTimeout;
        }

        public Builder setConnectionTimeout(Integer connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Integer getTimeOut() {
            return timeOut;
        }

        public void setTrustAll(boolean xtrustAll) {
            this.trustAll = xtrustAll;
        }

        public Builder setTimeOut(Integer timeOut) {
            this.timeOut = timeOut;
            return this;
        }
    }

}
