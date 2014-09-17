package nl.vpro.api.client.resteasy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.api.rs.v3.page.PageRestService;
import nl.vpro.api.rs.v3.profile.ProfileRestService;

@Named
public class NpoApiClients extends AbstractApiClient {

    private final NpoApiAuthentication authentication;

    private final MediaRestService mediaRestServiceProxy;

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
        int connectionTimeout
    ) {
		super(connectionTimeout, 16, 10000);
        this.authentication = new NpoApiAuthentication(apiKey, secret, origin);
        baseUrl = apiBaseUrl + "api";
        mediaRestServiceProxy = getTarget(baseUrl).proxyBuilder(MediaRestService.class).defaultConsumes(MediaType.APPLICATION_XML_TYPE).build();
        pageRestServiceProxy  = getTarget(baseUrl).proxyBuilder(PageRestService.class).defaultConsumes(MediaType.APPLICATION_XML_TYPE).build();
        profileRestServiceProxy = getTarget(baseUrl).proxyBuilder(ProfileRestService.class).defaultConsumes(MediaType.APPLICATION_XML_TYPE).build();
    }

    public MediaRestService getMediaService() {
        return mediaRestServiceProxy;
    }

    public PageRestService getPageService() {
        return pageRestServiceProxy;
    }

    public ProfileRestService getProfileService() {
        return profileRestServiceProxy;
    }

    private ResteasyWebTarget getTarget(String url) {
        ResteasyClient client =
            new ResteasyClientBuilder()
                .httpEngine(clientHttpEngine)
                .register(authentication)
                .build();
        return client.target(url);
    }

	@Override
	public String toString() {
		return super.toString() + " " + baseUrl;
	}

}
