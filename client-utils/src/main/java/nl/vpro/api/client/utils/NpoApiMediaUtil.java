package nl.vpro.api.client.utils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.ws.rs.ProcessingException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.api.Change;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.MediaResult;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaProvider;
import nl.vpro.domain.media.MediaType;
import nl.vpro.jackson2.JsonArrayIterator;

import static nl.vpro.api.client.utils.MediaRestClientUtils.unwrapIO;

/**
 * @author Michiel Meeuwissen
 */
@Named
public class NpoApiMediaUtil implements MediaProvider {

    final NpoApiClients clients;
    final NpoApiRateLimiter limiter;

    // TODO arrange caching via ehcache (ehcache4guice or something)

    private int cacheSize = 500;
    private int ttlInMinutes = 5;

    LoadingCache<String, Optional<MediaObject>> cache = buildCache();

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
    @Named("npo-api-mediautil.cachesize")
    public void setCacheSize(int size) {
        cacheSize = size;
        cache = buildCache();
    }

    @Named("npo-api-mediautil.cacheExpiryInMinutes")
    public void setCacheExpiry(int ttlInMinutes) {
        this.ttlInMinutes = ttlInMinutes;
        cache = buildCache();
    }

    private LoadingCache<String, Optional<MediaObject>> buildCache() {
        return CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .maximumSize(cacheSize)
            .expireAfterWrite(ttlInMinutes, TimeUnit.MINUTES)
            .build(
                new CacheLoader<String, Optional<MediaObject>>() {
                    @Override
                    public Optional<MediaObject> load(@NotNull String mid) throws IOException {
                        limiter.acquire();
                        try {
                            MediaObject object = MediaRestClientUtils.loadOrNull(clients.getMediaService(), mid);
                            limiter.upRate();
                            //return Optional.ofNullable(object);
                            return Optional.ofNullable(object);
                        } catch (IOException | RuntimeException se) {
                            limiter.downRate();
                            throw se;
                        }
                    }
                });
    }

    public MediaObject loadOrNull(String id) throws IOException {
        try {
            return cache.get(id).orElse(null);
        } catch (ExecutionException | UncheckedExecutionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            if (e.getCause() instanceof  RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException(e);
        }
    }

    public MediaResult listDescendants(String mid, Order order) {
        limiter.acquire();
        try {
            MediaResult result = clients.getMediaService().listDescendants(mid, null, order.toString(), 0L, 200);
            limiter.upRate();
            return result;
        } catch (Exception e) {
            limiter.downRate();
            throw e;
        }
    }

    public MediaResult unPage(BiFunction<Integer, Long, MediaResult> supplier, Predicate<MediaObject> filter, int max) {
        limiter.acquire();
        if (filter == null) {
            filter = new Predicate<MediaObject>() {
                @Override
                public boolean test(MediaObject mediaObject) {
                    return true;

                }
            };
        }
        try {
            List<MediaObject> result = new ArrayList<>();
            long offset = 0L;
            int batch = 50;

            long total;
            long found = 0;
            do {
                MediaResult list = supplier.apply(batch, offset);
                total = list.getTotal();
                list.getItems().stream().filter(filter).forEach(o -> {
                    if (result.size() < max) {
                        result.add(o);
                    }
                });
                offset += batch;
                found += list.getSize();
            } while (found < total && result.size() < max);

            limiter.upRate();
            return new MediaResult(result, 0L, max, total);
        } catch (Exception e) {
            limiter.downRate();
            throw e;
        }
    }

    public MediaResult listDescendants(String mid, Order order, Predicate<MediaObject> filter, int max) {
        BiFunction<Integer, Long, MediaResult> descendants = new BiFunction<Integer, Long, MediaResult>() {
            @Override
            public MediaResult apply(Integer batch, Long offset) {
                return clients.getMediaService().listDescendants(mid, null, order.toString(), offset, batch);
            }
        };
        return unPage(descendants, filter, max);
    }

    public MediaResult listMembers(String mid, Order order, Predicate<MediaObject> filter, int max) {
        BiFunction<Integer, Long, MediaResult> members = new BiFunction<Integer, Long, MediaResult>() {
            @Override
            public MediaResult apply(Integer batch, Long offset) {
                return clients.getMediaService().listMembers(mid, null, order.toString(), offset, batch);
            }
        };
        return unPage(members, filter, max);
    }


    public MediaObject[] load(String... id) throws IOException {
        Optional<MediaObject>[] result = new Optional[id.length];
        Set<String> toRequest = new LinkedHashSet<>();
        for (int i = 0; i < id.length; i++) {
            result[i] = cache.getIfPresent(id[i]);
            if (result[i] == null) {
                toRequest.add(id[i]);
            }
        }
        if (!toRequest.isEmpty()) {
            limiter.acquire();
            try {
                String[] array = toRequest.toArray(new String[toRequest.size()]);
                MediaObject[] requested = MediaRestClientUtils.load(clients.getMediaService(), array);
                for (int j = 0 ; j < array.length; j++) {
                    Optional<MediaObject> optional = Optional.ofNullable(requested[j]);
                    cache.put(array[j], optional);
                    for (int i = 0; i < id.length; i++) {
                        if (id[i].equals(array[j])) {
                            result[i] = optional;
                        }
                    }
                }
                limiter.upRate();
            } catch (ProcessingException pe) {
                limiter.downRate();
                unwrapIO(pe);
                throw pe;
            } catch (RuntimeException rte) {
                limiter.downRate();
                throw rte;
            }
        }
        MediaObject[] resultArray = new MediaObject[id.length];
        for (int i = 0; i < id.length; i++) {
            resultArray[i] = result[i].orElse(null);
        }
        return resultArray;
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
    public <T extends MediaObject> T findByMid(String mid) {
        try {
            return (T) load(mid)[0];
        } catch (IOException e) {
            return null;
        }
    }

    public MediaType getType(String mid) throws IOException {
		MediaObject object = load(mid)[0];
        return object == null ? MediaType.MEDIA : object.getMediaType();
    }

    @Override
    public String toString() {
        return String.valueOf(clients);
    }

    public NpoApiClients getClients() {
        return clients;
    }
}
