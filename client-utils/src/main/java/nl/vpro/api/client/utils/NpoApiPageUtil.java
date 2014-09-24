package nl.vpro.api.client.utils;

import javax.inject.Inject;
import javax.inject.Named;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.api.MultiplePageResult;
import nl.vpro.domain.page.Page;

/**
 * @author Michiel Meeuwissen
 */
@Named
public class NpoApiPageUtil  {

    final NpoApiClients clients;
    final NpoApiRateLimiter limiter;


    @Inject
    public NpoApiPageUtil(NpoApiClients clients, NpoApiRateLimiter limiter) {
        this.clients = clients;
        this.limiter = limiter;
    }

    public Page[] load(String... id) {
        MultiplePageResult pageResult = clients.getPageService().loadMultiple(id, null);

        Page[] result = new Page[id.length];
        for (int i = 0; i < id.length; i++) {
            result[i] = pageResult.getItems().get(i).getResult();
        }
        return result;
    }

    @Override
    public String toString() {
        return String.valueOf(clients);
    }


}
