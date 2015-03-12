package nl.vpro.api.client.utils;

import javax.inject.Inject;
import javax.inject.Named;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.api.SuggestResult;
import nl.vpro.domain.api.suggest.Query;
import nl.vpro.domain.api.suggest.QuerySearchRepository;

/**
 * @author Michiel Meeuwissen
 * @since 1.10
 */
@Named("mediaQueryRepository")
public class ApiClientMediaQuerySearchRepository implements QuerySearchRepository {

    final NpoApiClients clients;

    @Inject
    public ApiClientMediaQuerySearchRepository(NpoApiClients clients) {
        this.clients = clients;
    }

    @Override
    public void index(Query query, Type type) {

    }

    @Override
    public SuggestResult suggest(String input, String profile, Integer max) {
        return clients.getMediaService().suggest(input, profile, max);
    }
}
