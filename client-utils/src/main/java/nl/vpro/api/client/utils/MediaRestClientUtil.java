package nl.vpro.api.client.utils;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.api.Change;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.media.MediaObject;

/**
 * @author Michiel Meeuwissen
 */
public class MediaRestClientUtil {


    final NpoApiClients clients;

    @Inject
    public MediaRestClientUtil(NpoApiClients clients) {
        this.clients = clients;
    }

    public MediaObject loadOrNull(String id) {
        return MediaRestClientUtils.loadOrNull(clients.getMediaService(), id);
    }

    public MediaObject[] load(String... ids) {
        return MediaRestClientUtils.load(clients.getMediaService(), ids);
    }

    public List<Change> changes(String profile, long since, Order order, Integer max) throws IOException {
        return MediaRestClientUtils.changes(clients.getMediaService(), profile, since, order, max);
    }

    @Override
    public String toString() {
        return String.valueOf(clients);
    }


}
