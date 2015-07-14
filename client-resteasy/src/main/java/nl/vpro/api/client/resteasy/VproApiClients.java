package nl.vpro.api.client.resteasy;

import nl.vpro.api.rs.v3.schedule.ScheduleRestService;
import nl.vpro.resteasy.JacksonContextResolver;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;

@Named
public class VproApiClients extends AbstractApiClient {

    private final ScheduleRestService scheduleServiceProxy;

    private final String baseUrl;

    @Inject
    public VproApiClients(
            @Named("vpro-api.url") String apiBaseUrl,
            @Named("vpro-api.user") String user,
            @Named("vpro-api.password") String password,
            @Named("vpro-api.connectionTimeout") int connectionTimeout
    ) {
        super(connectionTimeout, 16, 3);
        baseUrl = apiBaseUrl + "/v3/api";

        BasicAuthentication authentication = new BasicAuthentication(user, password);
        ResteasyClient client = new ResteasyClientBuilder()
                .httpEngine(clientHttpEngine)
                .register(authentication)
                .register(JacksonContextResolver.class)
                .build();
        ResteasyWebTarget target = client.target(baseUrl);

        scheduleServiceProxy = target.proxyBuilder(ScheduleRestService.class)
                .defaultConsumes(MediaType.APPLICATION_XML_TYPE)
                .build();
    }

    public ScheduleRestService getScheduleService() {
        return scheduleServiceProxy;
    }

    @Override
    public String toString() {
        return "API client for " + baseUrl;
    }
}
