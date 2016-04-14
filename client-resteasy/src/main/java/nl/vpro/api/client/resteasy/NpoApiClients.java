package nl.vpro.api.client.resteasy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;

import java.lang.reflect.Proxy;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.api.rs.v3.page.PageRestService;
import nl.vpro.api.rs.v3.profile.ProfileRestService;
import nl.vpro.api.rs.v3.schedule.LeaveDefaultsProxyHandler;
import nl.vpro.api.rs.v3.schedule.ScheduleRestService;
import nl.vpro.api.rs.v3.schedule.ScheduleRestServiceWithDefaults;
import nl.vpro.resteasy.JacksonContextResolver;

@Named
public class NpoApiClients extends AbstractApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(NpoApiClients.class);

    private final ApiAuthenticationRequestFilter authentication;

    private final MediaRestService mediaRestServiceProxy;
    private final MediaRestService mediaRestServiceProxyNoTimeout;

    private final PageRestService pageRestServiceProxy;

    private final ScheduleRestServiceWithDefaults scheduleRestServiceProxy;

    private final ProfileRestService profileRestServiceProxy;

	private final String baseUrl;

    @Inject
    public NpoApiClients(
        @Named("npo-api.baseUrl")
        String apiBaseUrl,
        @Named("npo-api.apiKey")
        String apiKey,
        @Named("npo-api.secret")
        String secret,
        @Named("npo-api.origin")
        String origin,
        @Named("npo-api.connectionTimeout")
        Integer connectionTimeout
    ) {
		super(connectionTimeout, 16, 3);
        this.authentication = new ApiAuthenticationRequestFilter(apiKey, secret, origin);
        baseUrl = apiBaseUrl + "api";

        mediaRestServiceProxy =
            build(clientHttpEngine, MediaRestService.class);

        mediaRestServiceProxyNoTimeout =
            build(clientHttpEngineNoTimeout, MediaRestService.class);

        scheduleRestServiceProxy =
            build(clientHttpEngine, ScheduleRestServiceWithDefaults.class, ScheduleRestService.class);


        pageRestServiceProxy  =
            build(clientHttpEngine, PageRestService.class);

        profileRestServiceProxy =
            build(clientHttpEngine, ProfileRestService.class);

    }

    public NpoApiClients(
        String apiBaseUrl,
        String apiKey,
        String secret,
        String origin
    ) {
        this(apiBaseUrl, apiKey, secret, origin, 10);
    }


    public MediaRestService getMediaService() {
        return mediaRestServiceProxy;
    }

    public MediaRestService getMediaServiceNoTimeout() {
        return mediaRestServiceProxyNoTimeout;
    }

    public ScheduleRestServiceWithDefaults getScheduleService() {
        return scheduleRestServiceProxy;
    }

    public PageRestService getPageService() {
        return pageRestServiceProxy;
    }

    public ProfileRestService getProfileService() {
        return profileRestServiceProxy;
    }

    public ApiAuthenticationRequestFilter getAuthentication() {
        return authentication;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    protected String getInfo() {
        return getBaseUrl() + "/";
    }

	@Override
	public String toString() {
		return super.toString() + " " + baseUrl;
	}

    private <T, S> T build(ClientHttpEngine engine, Class<T> service, Class<S> restEasyService) { 
        T proxy;
        if (restEasyService == null) {
            proxy = builderResteasy(engine, service);
        } else {
            S resteasy = builderResteasy(engine, restEasyService);
            proxy = (T) Proxy.newProxyInstance(NpoApiClients.class.getClassLoader(),
                new Class[]{restEasyService, service}, new LeaveDefaultsProxyHandler(resteasy));
        }
            
        return
            ErrorAspect.proxyErrors(
                LOG,
                NpoApiClients.this::getInfo,
                service,
                proxy);
    }

    private <T> T build(ClientHttpEngine engine, Class<T> service) {
        return build(engine, service, null);
    }
    
    private <T> T builderResteasy(ClientHttpEngine engine, Class<T> service) {
        return getTarget(engine)
            .proxyBuilder(service)
            .defaultConsumes(MediaType.APPLICATION_XML)
            .defaultProduces(MediaType.APPLICATION_XML)
            .build();
    }

    private ResteasyWebTarget getTarget(ClientHttpEngine engine) {
        ResteasyClient client =
            new ResteasyClientBuilder()
                .httpEngine(engine)
                .register(authentication)
                .register(JacksonContextResolver.class)
                .build();
        return client.target(baseUrl);
    }

}
