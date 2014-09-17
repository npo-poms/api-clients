package nl.vpro.api.client.utils;

import java.io.IOException;
import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.api.Change;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaProvider;
import nl.vpro.domain.media.MediaType;

/**
 * @author Michiel Meeuwissen
 */
@Named
public class NpoApiMediaUtil implements MediaProvider {


    final NpoApiClients clients;
    final NpoApiRateLimiter limiter;


    @Inject
    public NpoApiMediaUtil(NpoApiClients clients, NpoApiRateLimiter limiter) {
        this.clients = clients;
        this.limiter = limiter;
    }


    public NpoApiMediaUtil(NpoApiClients clients) {
        this(clients, new NpoApiRateLimiter());
    }


    public MediaObject loadOrNull(String id) {
        limiter.acquire();
        try {
            MediaObject object = MediaRestClientUtils.loadOrNull(clients.getMediaService(), id);
            limiter.upRate();
            return object;
        } catch (RuntimeException rte) {
            limiter.downRate();
            throw rte;
        }
    }

    public MediaObject[] load(String... ids) {
        limiter.acquire();
        try {
            MediaObject[] result = MediaRestClientUtils.load(clients.getMediaService(), ids);
            limiter.upRate();
            return result;
        } catch (RuntimeException rte) {
            limiter.downRate();
            throw rte;
        }
    }

    public CloseableIterator<Change> changes(String profile, long since, Order order, Integer max) {
        limiter.acquire();
        try {
            CloseableIterator<Change> result = MediaRestClientUtils.changes(clients.getMediaService(), profile, since, order, max);
            limiter.upRate();
            return result;
        } catch (IOException e) {
            limiter.downRate();
            throw new RuntimeException(clients + ":" + e.getMessage(), e);
        }
    }

    public ProfileDefinition<MediaObject> getMediaProfile(String profile) {
        limiter.acquire();
        return clients.getProfileService().load(profile, null).getMediaProfile();
    }

    @Deprecated
    public Iterator<MediaObject> iterate(MediaForm form, String profile)  {
        limiter.acquire();
        try {
            Iterator<MediaObject> result = MediaRestClientUtils.iterate(clients.getMediaService(), form, profile);
            limiter.upRate();
            return result;
        } catch (Throwable e) {
            limiter.downRate();
            throw new RuntimeException(clients + ":" + e.getMessage(), e);
        }
    }

    @Deprecated
    public String toMid(String urn) {
        return MediaRestClientUtils.toMid(clients.getMediaService(), urn);
    }


    @Override
    public MediaObject findByMid(String mid) {
        return load(mid)[0];
    }

    public MediaType getType(String mid) {
        return load(mid)[0].getMediaType();
    }

    @Override
    public String toString() {
        return String.valueOf(clients);
    }


    NpoApiClients getClients() {
        return clients;
    }

}
