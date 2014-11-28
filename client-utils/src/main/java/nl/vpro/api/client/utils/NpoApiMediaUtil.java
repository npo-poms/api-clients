package nl.vpro.api.client.utils;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.ws.rs.ProcessingException;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.api.Change;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaProvider;
import nl.vpro.domain.media.MediaType;

import static nl.vpro.api.client.utils.MediaRestClientUtils.unwrapIO;

/**
 * @author Michiel Meeuwissen
 */
@Named
public class NpoApiMediaUtil implements MediaProvider {


    final NpoApiClients clients;
    final NpoApiRateLimiter limiter;

    // TODO arrange caching via ehcache (ehcache4guice or something)

    final LoadingCache<String, Optional<MediaObject>> cache = CacheBuilder.newBuilder()
        .concurrencyLevel(4)
        .maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(
            new CacheLoader<String, Optional<MediaObject>>() {
                @Override
                public Optional<MediaObject> load(@NotNull String mid) throws IOException {
                    limiter.acquire();
                    try {
                        MediaObject object = MediaRestClientUtils.loadOrNull(clients.getMediaService(), mid);
                        limiter.upRate();
                        //return Optional.ofNullable(object);
                        return Optional.fromNullable(object);
                    } catch (IOException | RuntimeException se ){
                        limiter.downRate();
                        throw se;
                    }
                }
            });


    @Inject
    public NpoApiMediaUtil(NpoApiClients clients, NpoApiRateLimiter limiter) {
        this.clients = clients;
        this.limiter = limiter;
    }


    public NpoApiMediaUtil(NpoApiClients clients) {
        this(clients, new NpoApiRateLimiter());
    }

    public void clearCache() {
        cache.invalidateAll();
    }


    public MediaObject loadOrNull(String id) throws IOException {
        try {
            //return cache.get(id).orElse(null);
            return cache.get(id).orNull();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new RuntimeException(e);
        }

    }

    public MediaObject[] load(String... ids) throws IOException {
        limiter.acquire();
        try {
            MediaObject[] result = MediaRestClientUtils.load(clients.getMediaService(), ids);
            limiter.upRate();
            return result;
        } catch (ProcessingException pe) {
            limiter.downRate();
            unwrapIO(pe);
            throw pe;
        } catch (RuntimeException rte) {
            limiter.downRate();
            throw rte;
        }
    }

    public JsonArrayIterator<Change> changes(String profile, long since, Order order, Integer max) {
        limiter.acquire();
        try {
            JsonArrayIterator<Change> result = MediaRestClientUtils.changes(clients.getMediaServiceNoTimeout(), profile, since, order, max);
            limiter.upRate();
            return result;
        } catch (IOException e) {
            limiter.downRate();
            throw new RuntimeException(clients + ":" + e.getMessage(), e);
        }
    }


    @Deprecated
    public Iterator<MediaObject> iterate(MediaForm form, String profile)  {
        limiter.acquire();
        try {
            Iterator<MediaObject> result = MediaRestClientUtils.iterate(clients.getMediaServiceNoTimeout(), form, profile);
            limiter.upRate();
            return result;
        } catch (Throwable e) {
            limiter.downRate();
            throw new RuntimeException(clients + ":" + e.getMessage(), e);
        }
    }


    @Override
    public MediaObject findByMid(String mid) {
        try {
            return load(mid)[0];
        } catch (IOException e) {
            return null;
        }
    }

    public MediaType getType(String mid) throws IOException {
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
