package nl.vpro.api.client.resteasy;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.api.rs.v3.page.PageRestService;
import nl.vpro.resteasy.JacksonContextResolver;

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
        mediaRestServiceProxy = target.proxyBuilder(MediaRestService.class).defaultConsumes(MediaType.APPLICATION_XML).build();
    }

    private void initPageRestServiceProxy(String url, ClientHttpEngine clientHttpEngine) {
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(clientHttpEngine).register(authentication).build();
        ResteasyWebTarget target = client.target(url);
        pageRestServiceProxy = target.proxyBuilder(PageRestService.class).defaultConsumes(MediaType.APPLICATION_XML).build();
    }
}
