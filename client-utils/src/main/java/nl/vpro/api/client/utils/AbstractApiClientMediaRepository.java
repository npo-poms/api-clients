package nl.vpro.api.client.utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.api.Change;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.MediaResult;
import nl.vpro.domain.api.media.ProgramResult;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.util.FilteringIterator;

/**
 * @author Michiel Meeuwissen
 * @since 1.1
 */
public class AbstractApiClientMediaRepository {
    private final static Logger LOG = LoggerFactory.getLogger(AbstractApiClientMediaRepository.class);

    final NpoApiClients clients;
    final NpoApiMediaUtil util;

    @Inject
    AbstractApiClientMediaRepository(NpoApiMediaUtil util) {
        this.util = util;
        this.clients = util.getClients();

    }

    public MediaObject load(String id)  {
        try {
            return util.loadOrNull(id);
        } catch (IOException e) {
            LOG.error(e.getMessage());
            return null;
        }
    }

    public List<MediaObject> loadAll(List<String> ids) {
        try {
            return Arrays.asList(util.load(ids.toArray(new String[ids.size()])));
        } catch (IOException e) {
            LOG.error(e.getMessage());
            return Collections.nCopies(ids.size(), null);
        }
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


    public MediaResult list(Order order, Long offset, Integer max) {
        return clients.getMediaService().list(null, order.toString(), offset, max);
    }

    public Iterator<MediaObject> iterate(ProfileDefinition<MediaObject> profile, MediaForm form, Integer max, Long offset, FilteringIterator.KeepAlive keepAlive) {
        throw new UnsupportedOperationException();
    }
}
