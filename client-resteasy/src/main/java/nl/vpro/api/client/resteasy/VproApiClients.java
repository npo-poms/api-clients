package nl.vpro.api.client.resteasy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import nl.vpro.api.rs.client.v3.schedule.ScheduleRestService;
import nl.vpro.resteasy.JacksonContextResolver;
import nl.vpro.rs.persons.PersonPublisherRestService;
import nl.vpro.rs.persons.PersonRestService;
import nl.vpro.rs.tips.TipPublisherRestService;
import nl.vpro.rs.tips.TipRestService;

@Named
public class VproApiClients extends AbstractApiClient {

    private final ScheduleRestService scheduleRestServiceProxy;

    private final TipRestService tipRestServiceProxy;

    private final TipPublisherRestService tipPublisherRestServiceProxy;

    private final PersonRestService personRestServiceProxy;

    private final PersonPublisherRestService personPublisherRestServiceProxy;

    private final String baseUrl;

    @Inject
    public VproApiClients(
            @Named("vpro-api.url") String apiBaseUrl,
            @Named("vpro-api.user") String user,
            @Named("vpro-api.password") String password,
            @Named("vpro-api.connectionTimeout") Integer connectionTimeout
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

        scheduleRestServiceProxy = target.proxyBuilder(ScheduleRestService.class)
                .defaultConsumes(MediaType.APPLICATION_XML_TYPE)
                .build();

        tipRestServiceProxy = target.proxyBuilder(TipRestService.class)
                .defaultConsumes(MediaType.APPLICATION_XML_TYPE)
                .build();

        tipPublisherRestServiceProxy = target.proxyBuilder(TipPublisherRestService.class)
                .defaultConsumes(MediaType.APPLICATION_XML_TYPE)
                .build();

        personRestServiceProxy = target.proxyBuilder(PersonRestService.class)
            .defaultConsumes(MediaType.APPLICATION_XML_TYPE)
            .build();
        personPublisherRestServiceProxy = target.proxyBuilder(PersonPublisherRestService.class)
            .defaultConsumes(MediaType.APPLICATION_XML_TYPE)
            .build();
    }

    public ScheduleRestService getScheduleRestService() {
        return scheduleRestServiceProxy;
    }

    public TipRestService getTipRestService() {
        return tipRestServiceProxy;
    }

    public TipPublisherRestService getTipPublisherRestService() {
        return tipPublisherRestServiceProxy;
    }

    public PersonRestService getPersonRestService() {
        return personRestServiceProxy;
    }

    public PersonPublisherRestService getPersonPublisherRestService() {
        return personPublisherRestServiceProxy;
    }

    @Override
    public String toString() {
        return "API client for " + baseUrl;
    }
}
