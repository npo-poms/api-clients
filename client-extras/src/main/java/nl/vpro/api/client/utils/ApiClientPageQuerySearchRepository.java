package nl.vpro.api.client.utils;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import nl.vpro.api.client.frontend.NpoApiClients;
import nl.vpro.domain.api.SuggestResult;
import nl.vpro.domain.api.suggest.Query;
import nl.vpro.domain.api.suggest.QuerySearchRepository;

/**
 * @author Michiel Meeuwissen
 * @since 1.10
 */
@Named("pageQueryRepository")
public class ApiClientPageQuerySearchRepository implements QuerySearchRepository {

    final NpoApiClients clients;

    @Inject
    public ApiClientPageQuerySearchRepository(NpoApiClients clients) {
        this.clients = clients;
    }

    @Override
    public void index(Query query) {

    }

    @Override
    public SuggestResult suggest(String input, String profile, Integer max) {
        return clients.getPageService().suggest(input, profile, max);
    }
}
