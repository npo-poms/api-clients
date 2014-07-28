package nl.vpro.api.client.resteasy;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.api.rs.v3.page.PageRestService;
import nl.vpro.api.rs.v3.profile.ProfileRestService;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;

public class NpoApiClients extends AbstractApiClient {
    private final NpoApiAuthentication authentication;

    private MediaRestService mediaRestServiceProxy;

    private PageRestService pageRestServiceProxy;

    private ProfileRestService profileRestServiceProxy;


    @Inject
    public NpoApiClients(
        @Named("npo-api.baseUrl") String apiBaseUrl,
        @Named("npo-api.apiKey") String apiKey,
        @Named("npo-api.secret") String secret,
        @Named("npo-api.origin") String origin
    ) {
        this(apiBaseUrl, new NpoApiAuthentication(apiKey, secret, origin), 10000, 16, 10000);
    }

    private NpoApiClients(String apiBaseUrl, NpoApiAuthentication authentication, int connectionTimeoutMillis, int maxConnections, int connectionInPoolTTL) {
        super(connectionTimeoutMillis, maxConnections, connectionInPoolTTL);

        this.authentication = authentication;

        String baseUrl = apiBaseUrl + "api";
        initMediaRestServiceProxy(baseUrl);
        initPageRestServiceProxy(baseUrl);
        initProfileRestServiceProxy(baseUrl);
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
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(clientHttpEngine).register(authentication).build();
        return client.target(url);
    }

    private void initMediaRestServiceProxy(String url) {
        mediaRestServiceProxy = getTarget(url).proxyBuilder(MediaRestService.class).defaultConsumes(MediaType.APPLICATION_JSON).build();
    }

    private void initPageRestServiceProxy(String url) {
        pageRestServiceProxy = getTarget(url).proxyBuilder(PageRestService.class).defaultConsumes(MediaType.APPLICATION_JSON).build();
    }

    private void initProfileRestServiceProxy(String url) {
        pageRestServiceProxy = getTarget(url).proxyBuilder(PageRestService.class).defaultConsumes(MediaType.APPLICATION_JSON).build();
    }
}
