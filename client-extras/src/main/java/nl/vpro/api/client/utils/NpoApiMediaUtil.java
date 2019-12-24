package nl.vpro.api.client.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;

import com.google.common.cache.*;
import com.google.common.util.concurrent.UncheckedExecutionException;

import nl.vpro.api.client.frontend.NpoApiClients;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.media.*;
import nl.vpro.domain.media.*;
import nl.vpro.jackson2.JsonArrayIterator;
import nl.vpro.util.CloseableIterator;
import nl.vpro.util.TimeUtils;

import static nl.vpro.api.client.utils.MediaRestClientUtils.unwrapIO;
import static nl.vpro.domain.api.Result.Total.equalsTo;

/**
 * Wrapper around {@link NpoApiClients}, that provides things like:
 * <ul>
 <li>rate limiting</li>
 <li>caching</li>
 <li>un paging of calls that require paging. if the api enforces a max of at most e.g. 240, calls in this utility will accept any max, and do paging implicitely</li>
 <li></li>less arguments. Some of the Rest service interface want arguments like request and response object which should at the client side simply remain null (btw I think there are no much of that kind of methods left</li>
 <li>exception handling</li>
 <li>Parsing of input stream if that it the return value (huge results like {@link nl.vpro.api.rs.v3.media.MediaRestService#changes(String, String, Long, String, String, Integer, Boolean, Deletes)} and {@link nl.vpro.api.rs.v3.media.MediaRestService#iterate(MediaForm, String, String, Long, Integer)} have that.</li>
 </ul>

 * @author Michiel Meeuwissen
 */
@Named
@Slf4j
public class NpoApiMediaUtil implements MediaProvider {

    final NpoApiClients clients;
    final NpoApiRateLimiter limiter;

    // TODO arrange caching via ehcache (ehcache4guice or something)

    private int cacheSize = 500;
    private Duration cacheTTL = Duration.ofMinutes(5);


    private boolean iterateLogProgress = true;

    LoadingCache<String, Optional<? extends MediaObject>> cache = buildCache();

    @Inject
    public NpoApiMediaUtil(@NotNull NpoApiClients clients, @NotNull NpoApiRateLimiter limiter) {
        this.clients = clients;
        this.limiter = limiter;
    }


    @lombok.Builder
    private  NpoApiMediaUtil(
        @NotNull NpoApiClients clients,
        @NotNull NpoApiRateLimiter limiter,
        boolean iterateLogProgress,
        int cacheSize,
        Duration cacheTTL
        ) {
        this.clients = clients;
        this.limiter = limiter == null ? new NpoApiRateLimiter() : limiter;
        this.cacheTTL = cacheTTL;
        this.cacheSize = cacheSize;
        this.iterateLogProgress = iterateLogProgress;
    }

    public NpoApiMediaUtil(NpoApiClients clients) {
        this(clients, new NpoApiRateLimiter());
    }

    public void clearCache() {
        cache.invalidateAll();
        //clients.clearBrowserCache();

    }
    @Named("npo-api-mediautil.cachesize")
    public void setCacheSize(int size) {
        cacheSize = size;
        cache = buildCache();
    }

    @Named("npo-api-mediautil.cacheExpiry")
    public void setCacheExpiry(String ttl) {
        this.cacheTTL = TimeUtils.parseDuration(ttl).orElse(Duration.ofMinutes(5));
        cache = buildCache();
    }

    private LoadingCache<String, Optional<? extends MediaObject>> buildCache() {
        return CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .maximumSize(cacheSize)
            .expireAfterWrite(cacheTTL.toMillis(), TimeUnit.MILLISECONDS)
            .build(
                new CacheLoader<String, Optional<? extends MediaObject>>() {
                    @Override
                    public Optional<MediaObject> load(@NotNull String mid) {
                        limiter.acquire();
                        try {
                            MediaObject object = MediaRestClientUtils.loadOrNull(clients.getMediaService(), mid);
                            limiter.upRate();
                            //return Optional.ofNullable(object);
                            return Optional.ofNullable(object);
                        } catch (RuntimeException se) {
                            limiter.downRate();
                            throw se;
                        }
                    }
                });
    }

    public <T extends MediaObject> T loadOrNull(String id) throws IOException {
        try {
            return (T) cache.get(id).orElse(null);
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

    public void invalidateCache() {
        cache.invalidateAll();
    }

    public MediaResult listDescendants(String mid, Order order) {
        limiter.acquire();
        try {
            MediaResult result = clients.getMediaService().listDescendants(mid, null, null, order.toString(), 0L, 200);
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
            filter = mediaObject -> true;
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
                if (list.getSize() == 0) {
                    break;
                }
                list.getItems().stream()
                    .filter(filter).forEach(o -> {
                    if (result.size() < max) {
                        result.add(o);
                    }
                });
                offset += batch;
                found += list.getSize();
            } while (found < total && result.size() < max);

            limiter.upRate();
            return new MediaResult(result, 0L, max, equalsTo(total));
        } catch (Exception e) {
            limiter.downRate();
            throw e;
        }
    }

    /**
     * The api has limits on max size, requiring you to use paging when you want more.
     * This method arranges that.
     */
    public ProgramResult unPageProgramResult(BiFunction<Integer, Long, ProgramResult> supplier, Predicate<MediaObject> filter, int max) {
        limiter.acquire();
        if (filter == null) {
            filter = mediaObject -> true;
        }
        try {
            List<Program> result = new ArrayList<>();
            long offset = 0L;
            int batch = 50;

            long total;
            long found = 0;
            do {
                ProgramResult list = supplier.apply(batch, offset);
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
            return new ProgramResult(result, 0L, max, equalsTo(total));
        } catch (Exception e) {
            limiter.downRate();
            throw e;
        }
    }


    /**
     * Wraps {@link nl.vpro.api.rs.v3.media.MediaRestService#listDescendants(String, String, String, String, Long, Integer)}, but with less arguments.
     * @param max The max number of results you want. If this is bigger than the maximum accepted by the API, implicit paging will happen.
     */
    public MediaResult listDescendants(String mid, Order order, Predicate<MediaObject> filter, int max) {
        BiFunction<Integer, Long, MediaResult> descendants = (batch, offset) ->
            clients.getMediaService().listDescendants(mid, null, null, order.toString(), offset, batch);
        return unPage(descendants, filter, max);
    }

    public MediaResult listMembers(String mid, Order order, Predicate<MediaObject> filter, int max) {
        BiFunction<Integer, Long, MediaResult> members =
            (batch, offset) -> clients.getMediaService().listMembers(mid, null, null,
                order.toString(), offset, batch);
        return unPage(members, filter, max);
    }

    public ProgramResult listEpisodes(String mid, Order order, Predicate<MediaObject> filter, int max) {
        BiFunction<Integer, Long, ProgramResult> members = (batch, offset) -> clients.getMediaService().listEpisodes(mid, null,null, order.toString(), offset, batch);
        return unPageProgramResult(members, filter, max);
    }

    @SuppressWarnings({"unchecked", "OptionalAssignedToNull"})
    public MediaObject[] load(String... id) throws IOException {
        Optional<? extends MediaObject>[] result = new Optional[id.length];
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
                String[] array = toRequest.toArray(new String[0]);
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

    public JsonArrayIterator<MediaChange> changes(String profile, Instant since, Order order, Integer max) {
        return changes(profile, since, null, order, max);
    }
    public JsonArrayIterator<MediaChange> changes(String profile, Instant since, String mid, Order order, Integer max) {
        return changes(profile, true, null, since, mid, order, max, Deletes.ID_ONLY);
    }

    public JsonArrayIterator<MediaChange> changes(String profile, boolean profileCheck, Instant since, String mid, Order order, Integer max, Deletes deletes) {
        return changes(profile, profileCheck, null, since, mid, order, max, deletes);
    }


    public RedirectList redirects() {
        try (Response response = clients.getMediaService().redirects(null)) {
            return response.readEntity(RedirectList.class);
        }

    }

    protected  JsonArrayIterator<MediaChange> changes(String profile, boolean profileCheck, Long sinceSequence, Instant since,  String mid, Order order, Integer max, Deletes deletes) {
        limiter.acquire();
        try {

            JsonArrayIterator<MediaChange> result;
            if (sinceSequence == null) {
                result = MediaRestClientUtils.changes(clients.getMediaServiceNoTimeout(), profile, profileCheck, since, mid, order, max, deletes);
            } else {
                result = MediaRestClientUtils.changes(clients.getMediaServiceNoTimeout(), profile, sinceSequence, order, max);
            }
            limiter.upRate();
            return result;
        } catch (IOException e) {
            limiter.downRate();
            throw new RuntimeException(clients + ":" + e.getMessage(), e);
        }
    }

    @Deprecated
    public JsonArrayIterator<MediaChange> changes(String profile, Long since, Order order, Integer max) {
        limiter.acquire();
        try {
            JsonArrayIterator<MediaChange> result = MediaRestClientUtils.changes(clients.getMediaServiceNoTimeout(), profile, since, order, max);
            limiter.upRate();
            return result;
        } catch (IOException e) {
            limiter.downRate();
            throw new RuntimeException(clients + ":" + e.getMessage(), e);
        }
    }

    /**
     * Calls {@link nl.vpro.api.rs.v3.media.MediaRestService#iterate(MediaForm, String, String, Long, Integer)}, and wraps the resulting {@link java.io.InputStream} in an {@link Iterator} of {@link MediaObject}}
     */
    public CloseableIterator<MediaObject> iterate(MediaForm form, String profile)  {
        limiter.acquire();
        try {
            CloseableIterator<MediaObject> result = MediaRestClientUtils
                .iterate(clients.getMediaServiceNoTimeout(), form, profile, iterateLogProgress);
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
