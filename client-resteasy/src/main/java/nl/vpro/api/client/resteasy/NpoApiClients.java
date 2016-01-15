package nl.vpro.api.client.resteasy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.api.rs.v3.page.PageRestService;
import nl.vpro.api.rs.v3.profile.ProfileRestService;
import nl.vpro.resteasy.JacksonContextResolver;

@Named
public class NpoApiClients extends AbstractApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(NpoApiClients.class);


    private final ApiAuthenticationRequestFilter authentication;

    private final MediaRestService mediaRestServiceProxy;
    private final MediaRestService mediaRestServiceProxyNoTimeout;


    private final PageRestService pageRestServiceProxy;

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
            proxyErrors(
                MediaRestService.class,
                getTarget(clientHttpEngine, baseUrl)
                    .proxyBuilder(MediaRestService.class)
                    .defaultConsumes(MediaType.APPLICATION_JSON + "; charset=utf-8")
                    .defaultProduces(MediaType.APPLICATION_XML)
                    .build());
        mediaRestServiceProxyNoTimeout =
            proxyErrors(
                MediaRestService.class,
                getTarget(clientHttpEngineNoTimeout, baseUrl)
                    .proxyBuilder(MediaRestService.class)
                    .defaultConsumes(MediaType.APPLICATION_XML_TYPE)
                    .build());

        pageRestServiceProxy  =
            proxyErrors(
                PageRestService.class,
                getTarget(clientHttpEngine, baseUrl)
                    .proxyBuilder(PageRestService.class)
                    .defaultConsumes(MediaType.APPLICATION_XML_TYPE)
                    .build()
            );
        profileRestServiceProxy =
            proxyErrors(
                ProfileRestService.class,
                getTarget(clientHttpEngine, baseUrl)
                    .proxyBuilder(ProfileRestService.class)
                    .defaultConsumes(MediaType.APPLICATION_XML_TYPE)
                    .build()
            )
        ;
    }

    <T> T proxyErrors(Class<T> clazz, T proxy) {
        return ErrorAspect.proxyErrors(LOG, NpoApiClients.this::getInfo, clazz, proxy);
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

    public PageRestService getPageService() {
        return pageRestServiceProxy;
    }

    public ProfileRestService getProfileService() {
        return profileRestServiceProxy;
    }

    public ApiAuthenticationRequestFilter getAuthentication() {
        return authentication;
    }

    private ResteasyWebTarget getTarget(ClientHttpEngine engine, String url) {
        ResteasyClient client =
            new ResteasyClientBuilder()
                .httpEngine(engine)
                .register(authentication)
                .register(JacksonContextResolver.class)
                .build();
        return client.target(url);
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

}
