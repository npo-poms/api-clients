package nl.vpro.api.client.resteasy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.api.rs.v3.page.PageRestService;

public class NpoApiClients extends AbstractApiClient {
    private final NpoApiAuthentication authentication;

    private MediaRestService mediaRestServiceProxy;

    private PageRestService pageRestServiceProxy;

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

        initMediaRestServiceProxy(apiBaseUrl, clientHttpEngine);
        initPageRestServiceProxy(apiBaseUrl, clientHttpEngine);
    }

    public MediaRestService getMediaService() {
        return mediaRestServiceProxy;
    }

    public PageRestService getPageService() {
        return pageRestServiceProxy;
    }

    private void initMediaRestServiceProxy(String url, ClientHttpEngine engine) {
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).register(authentication).build();
        ResteasyWebTarget target = client.target(url);
        mediaRestServiceProxy = target.proxyBuilder(MediaRestService.class).defaultConsumes(MediaType.APPLICATION_JSON).build();
    }

    private void initPageRestServiceProxy(String url, ClientHttpEngine clientHttpEngine) {
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(clientHttpEngine).register(authentication).build();
        ResteasyWebTarget target = client.target(url);
        pageRestServiceProxy = target.proxyBuilder(PageRestService.class).defaultConsumes(MediaType.APPLICATION_JSON).build();
    }
}
