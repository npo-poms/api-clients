package nl.vpro.api.client.utils;

import java.io.IOException;
import java.util.Iterator;

import javax.inject.Inject;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.api.Change;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.media.MediaObject;

/**
 * @author Michiel Meeuwissen
 */
public class NpoApiClientUtil {


    final NpoApiClients clients;

    @Inject
    public NpoApiClientUtil(NpoApiClients clients) {
        this.clients = clients;
    }

    public MediaObject loadOrNull(String id) {
        return MediaRestClientUtils.loadOrNull(clients.getMediaService(), id);
    }

    public MediaObject[] load(String... ids) {
        return MediaRestClientUtils.load(clients.getMediaService(), ids);
    }

    public Iterator<Change> changes(String profile, long since, Order order, Integer max) throws IOException {
        return MediaRestClientUtils.changes(clients.getMediaService(), profile, since, order, max);
    }

    @Deprecated
    public Iterator<MediaObject> iterate(MediaForm form, String profile) throws IOException {
        return MediaRestClientUtils.iterate(clients.getMediaService(), form, profile);
    }

    @Deprecated
    public String toMid(String urn) {
        return MediaRestClientUtils.toMid(clients.getMediaService(), urn);
    }

    @Override
    public String toString() {
        return String.valueOf(clients);
    }


}
