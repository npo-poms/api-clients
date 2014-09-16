package nl.vpro.api.client.utils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.api.Change;
import nl.vpro.domain.api.MediaResult;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.ProgramResult;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.media.MediaObject;

/**
 * @author Michiel Meeuwissen
 * @since 1.1
 */
public class AbstractApiClientMediaRepository {
    final NpoApiClients clients;
    final NpoApiClientUtil util;

    @Inject
    AbstractApiClientMediaRepository(NpoApiClients clients) {
        this.clients = clients;
        this.util = new NpoApiClientUtil(clients);
    }

    public MediaObject load(String id) {
        return util.loadOrNull(id);
    }

    public List<MediaObject> loadAll(List<String> ids) {
        return Arrays.asList(util.load(ids.toArray(new String[ids.size()])));
    }

    public Iterator<Change> changes(Long since, ProfileDefinition<MediaObject> current, ProfileDefinition<MediaObject> previous, Order order, Integer max, Long keepAlive) {
        throw new UnsupportedOperationException();
    }

    public MediaResult listDescendants(MediaObject media, Order order, Long offset, Integer max) {
        return clients.getMediaService().listDescendants(media.getMid(), null, order.toString(), offset, max);

    }

    public ProgramResult listEpisodes(MediaObject media, Order order, Long offset, Integer max) {
        return clients.getMediaService().listEpisodes(media.getMid(), null, order.toString(), offset, max);
    }

    public MediaResult listMembers(MediaObject media, Order order, Long offset, Integer max) {
        return clients.getMediaService().listMembers(media.getMid(), null, order.toString(), offset, max);

    }

    public Iterator<MediaObject> iterate(ProfileDefinition<MediaObject> profile, MediaForm form) {
        throw new UnsupportedOperationException();
    }

    public MediaResult list(Order order, Long offset, Integer max) {
        return clients.getMediaService().list(null, order.toString(), offset, max, "");
    }
}
