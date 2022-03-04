package nl.vpro.api.client.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXB;

import nl.vpro.api.client.frontend.NpoApiClients;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.media.*;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.api.profile.exception.ProfileNotFoundException;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaRedirector;
import nl.vpro.util.CloseableIterator;
import nl.vpro.util.FilteringIterator;

/**
 * @author Michiel Meeuwissen
 * @since 1.1
 */
@Slf4j
public abstract class AbstractApiClientMediaRepository implements MediaRepository, MediaRedirector {

    final NpoApiClients clients;
    final NpoApiMediaUtil util;

    @Inject
    AbstractApiClientMediaRepository(NpoApiMediaUtil util) {
        this.util = util;
        this.clients = util.getClients();

    }

    @Override
    public MediaObject load(boolean loadDeleted, String id)  {
        if (loadDeleted) {
            throw new UnsupportedOperationException("We don't support loading deleted objects");
        }
        try {
            return util.loadOrNull(id);
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @Override
    public List<MediaObject> loadAll(boolean loadDeleted, List<String> ids) {
        if (loadDeleted) {
            throw new UnsupportedOperationException("We don't support loading deleted objects");
        }
        try {
            return Arrays.asList(util.load(ids.toArray(new String[0])));
        } catch (IOException e) {
            log.error(e.getMessage());
            return Collections.nCopies(ids.size(), null);
        }
    }

    @Override
    public CloseableIterator<MediaChange> changes(Long since, ProfileDefinition<MediaObject> current, ProfileDefinition<MediaObject> previous, Order order, Integer max, Long keepAlive) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MediaResult listDescendants(MediaObject media, ProfileDefinition<MediaObject> profile, Order order, long offset, Integer max) throws ProfileNotFoundException {
        return clients.getMediaService().listDescendants(media.getMid(), name(profile), null, order.toString(), offset, max);

    }

    @Override
    public ProgramResult listEpisodes(MediaObject media, ProfileDefinition<MediaObject> profile, Order order, long offset, Integer max) throws ProfileNotFoundException {
        return clients.getMediaService().listEpisodes(media.getMid(), name(profile),  null, order.toString(), offset, max);
    }

    @Override
    public MediaResult listMembers(MediaObject media, ProfileDefinition<MediaObject> profile, Order order, long offset, Integer max) throws ProfileNotFoundException {
        return clients.getMediaService().listMembers(media.getMid(), name(profile), null, order.toString(), offset, max);

    }


    @Override
    public MediaResult list(Order order, long offset, Integer max) {
        return clients.getMediaService().list(null, order.toString(), offset, max);
    }

    public Iterator<MediaObject> iterate(ProfileDefinition<MediaObject> profile, MediaForm form, Integer max, long offset, FilteringIterator.KeepAlive keepAlive) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<String> redirect(String s) {
        MediaObject got = clients.getMediaService().load(s, "", null);
        if (got == null) {
            return Optional.empty();
        }
        if (Objects.equals(got.getMid(), s)) {
            return Optional.empty();
        }
        return Optional.ofNullable(got.getMid());
    }

    @Override
    public RedirectList redirects() {
        try (Response r = clients.getMediaService().redirects(null)) {
            InputStream is = (InputStream) r.getEntity();
            return JAXB.unmarshal(is, RedirectList.class);
        }
    }


    @Override
    public CloseableIterator<MediaChange> changes(Instant since, String mid, ProfileDefinition<MediaObject> current, ProfileDefinition<MediaObject> previous, Order order, Integer max, Long keepAlive, Deletes deletes, Tail tail) {
        //clients.getMediaService().changes(current.getName(), null, since)
        throw new UnsupportedOperationException();

    }

    public Long getCurrentSince() {
        return Instant.now().toEpochMilli();

    }
    @Override
    public CloseableIterator<MediaObject> iterate(ProfileDefinition<MediaObject> profile, MediaForm form, long offset, Integer max, FilteringIterator.KeepAlive keepAlive) {
        //InputStream i = clients.getMediaService().iterate(form, profile.getName(), null, offset, max, keepAlive, null, null);
        throw new UnsupportedOperationException();

    }

    protected String name(ProfileDefinition<MediaObject> profile) {
        return profile == null ? null : profile.getName();
    }

}

