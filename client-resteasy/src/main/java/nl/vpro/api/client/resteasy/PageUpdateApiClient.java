package nl.vpro.api.client.resteasy;

import nl.vpro.rs.pages.update.PageUpdateRestService;
import org.jboss.resteasy.client.jaxrs.*;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;

public class PageUpdateApiClient extends AbstractApiClient {
    private final BasicAuthentication authentication;

    private final PageUpdateRestService pageUpdateRestService;

    private final String toString;

    @Inject
    public PageUpdateApiClient(
        @Named("pageupdate-api.baseUrl") String apiBaseUrl,
        @Named("pageupdate-api.user") String user,
        @Named("pageupdate-api.password") String password
    ) {
        super(10000, 16, 10000);
        this.authentication = new BasicAuthentication(user, password);
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(clientHttpEngine).register(authentication).build();
        ResteasyWebTarget target = client.target(apiBaseUrl);
        pageUpdateRestService = target.proxyBuilder(PageUpdateRestService.class).defaultConsumes(MediaType.APPLICATION_XML).build();
        toString = user + "@" + apiBaseUrl;
    }

    public PageUpdateRestService getPageUpdateRestService() {
        return pageUpdateRestService;
    }

    @Override
    public String toString() {
        return getClass().getName() + " " + toString;
    }
}
