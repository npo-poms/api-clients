package nl.vpro.api.client.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import javax.inject.Inject;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.api.Change;
import nl.vpro.domain.api.Deletes;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.MediaResult;
import nl.vpro.domain.api.media.ProgramResult;
import nl.vpro.domain.api.media.RedirectList;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.util.FilteringIterator;

/**
 * @author Michiel Meeuwissen
 * @since 1.1
 */
@Slf4j
public class AbstractApiClientMediaRepository {

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
            log.error(e.getMessage());
            return null;
        }
    }

    public List<MediaObject> loadAll(List<String> ids) {
        try {
            return Arrays.asList(util.load(ids.toArray(new String[ids.size()])));
        } catch (IOException e) {
            log.error(e.getMessage());
            return Collections.nCopies(ids.size(), null);
        }
    }

    public Iterator<Change> changes(Long since, ProfileDefinition<MediaObject> current, ProfileDefinition<MediaObject> previous, Order order, Integer max, Long keepAlive) {
        throw new UnsupportedOperationException();
    }

    public MediaResult listDescendants(MediaObject media, Order order, long offset, Integer max) {
        return clients.getMediaService().listDescendants(media.getMid(), null, null, order.toString(), offset, max);

    }

    public ProgramResult listEpisodes(MediaObject media, Order order, long offset, Integer max) {
        return clients.getMediaService().listEpisodes(media.getMid(), null,  null, order.toString(), offset, max);
    }

    public MediaResult listMembers(MediaObject media, Order order, long offset, Integer max) {
        return clients.getMediaService().listMembers(media.getMid(), null, null, order.toString(), offset, max);

    }


    public MediaResult list(Order order, long offset, Integer max) {
        return clients.getMediaService().list(null, order.toString(), offset, max);
    }

    public Iterator<MediaObject> iterate(ProfileDefinition<MediaObject> profile, MediaForm form, Integer max, long offset, FilteringIterator.KeepAlive keepAlive) {
        throw new UnsupportedOperationException();
    }

	public Optional<String> redirect(String s) {
		MediaObject got = clients.getMediaService().load(s, "", null);
		if (got == null) {
		    return Optional.empty();
        }
		if (got.getMid().equals(s)) {
			return Optional.empty();
		}
		return Optional.of(got.getMid());
	}

    public RedirectList redirects() {
        return new RedirectList();

    }


    public Iterator<Change> changes(Instant since, String mid, ProfileDefinition<MediaObject> current, ProfileDefinition<MediaObject> previous, Order order, Integer max, Long keepAlive, Deletes deletes) {
        //clients.getMediaService().changes(current.getName(), null, since)
        throw new UnsupportedOperationException();

    }

    public Long getCurrentSince() {
        return Instant.now().toEpochMilli();

    }
    public Iterator<MediaObject> iterate(ProfileDefinition<MediaObject> profile, MediaForm form, long offset, Integer max, FilteringIterator.KeepAlive keepAlive) {
        //InputStream i = clients.getMediaService().iterate(form, profile.getName(), null, offset, max, keepAlive, null, null);
        throw new UnsupportedOperationException();

    }

}

