package nl.vpro.api.client.resteasy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.*;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.api.rs.v3.page.PageRestService;
import nl.vpro.rs.pages.update.PageUpdateRestService;

public class PageUpdateApiClient extends AbstractApiClient {
    private final BasicAuthentication authentication;

    private PageUpdateRestService pageUpdateRestService;

    @Inject
    public PageUpdateApiClient(
        @Named("pageupdate-api.baseUrl") String apiBaseUrl,
        @Named("pageupdate-api.user") String user,
        @Named("pageupdate-api.password") String password
    ) {
        this(apiBaseUrl, new BasicAuthentication(user, password), 10000, 16, 10000);
    }

    private PageUpdateApiClient(String apiBaseUrl, BasicAuthentication authentication, int connectionTimeoutMillis, int maxConnections, int connectionInPoolTTL) {
        super(connectionTimeoutMillis, maxConnections, connectionInPoolTTL);

        this.authentication = authentication;

        initPageUpdateRestServiceProxy(apiBaseUrl, clientHttpEngine);
    }

    public PageUpdateRestService getPageUpdateService() {
        return pageUpdateRestService;
    }

    private void initPageUpdateRestServiceProxy(String url, ClientHttpEngine engine) {
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).register(authentication).build();
        ResteasyWebTarget target = client.target(url);
        pageUpdateRestService = target.proxyBuilder(PageUpdateRestService.class).defaultConsumes(MediaType.APPLICATION_XML).build();
    }
}
