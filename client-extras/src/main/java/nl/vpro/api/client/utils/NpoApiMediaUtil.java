package nl.vpro.api.client.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ConnectException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.meeuw.functional.Consumers;

import com.google.common.cache.*;
import com.google.common.util.concurrent.UncheckedExecutionException;

import nl.vpro.api.client.frontend.NpoApiClients;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.media.*;
import nl.vpro.domain.media.*;
import nl.vpro.jackson2.JsonArrayIterator;
import nl.vpro.util.*;

import static nl.vpro.api.client.utils.ChangesFeedParameters.changesParameters;
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
 <li>Parsing of input stream if that it the return value (huge results like {@link nl.vpro.api.rs.v3.media.MediaRestService#changes(String, String, Long, String, String, Integer, Boolean, Deletes, Tail, String)} and {@link nl.vpro.api.rs.v3.media.MediaRestService#iterate(MediaForm, String, String, Long, Integer)} have that.</li>
 </ul>

 * @author Michiel Meeuwissen
 * @see NpoApiPageUtil
 * @see NpoApiImageUtil
 */
@Named
@Slf4j
public class NpoApiMediaUtil implements MediaProvider {


    protected static ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(1);


    final NpoApiClients clients;
    final NpoApiRateLimiter limiter;

    // TODO arrange caching via ehcache (ehcache4guice or something)

    private int cacheSize = 500;
    private Duration cacheTTL = Duration.ofMinutes(5);


    private boolean iterateLogProgress = true;

    LoadingCache<String, Optional<? extends MediaObject>> cache = buildCache();

    private static long maxWindow = 10000;

    private Instant loggedAboutConnect = Instant.EPOCH;

    @Inject
    public NpoApiMediaUtil(@NotNull NpoApiClients clients, @NotNull NpoApiRateLimiter limiter) {
        this.clients = clients;
        this.limiter = limiter;
    }


    @lombok.Builder
    private  NpoApiMediaUtil(
        @NotNull NpoApiClients clients,
        @Nullable NpoApiRateLimiter limiter,
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
                    public @NonNull Optional<MediaObject> load(@NonNull String mid) {
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

    @SuppressWarnings("unchecked")
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

    /**
     * Given an api with offset/max formalism, unpage this. The 'maximal' match of NPO API is
     * 240, this allows for large max sizes.
     */
    public MediaResult unPage(
        BiFunction<Integer, Long, MediaResult> supplier,
        Predicate<MediaObject> filter,
        int max) {
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
                if (offset > maxWindow - batch) {
                    log.info("Offset is getting to big. Breaking");
                    break;
                }
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
                if (list.getSize() == 0) {
                    break;
                }
                list.getItems().stream().filter(filter).forEach(o -> {
                    if (result.size() < max) {
                        result.add(o);
                    }
                });
                offset += batch;
                found += list.getSize();
                if (offset > maxWindow - batch) {
                    log.info("Offset is getting to big. Breaking");
                    break;
                }
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

    public CountedIterator<MediaChange> changes(String profile, Instant since, Order order, Integer max) {
        return changes(profile, since, null, order, max);
    }
    public CountedIterator<MediaChange> changes(String profile, Instant since, String mid, Order order, Integer max) {
        return changes(changesParameters()
            .profile(profile)
            .profileCheck(false)
            .since(since)
            .mid(mid)
            .order(order)
            .max(max)
            .deletes(Deletes.ID_ONLY)
            .tail(Tail.IF_EMPTY)
            .build());
    }

    public CountedIterator<MediaChange> changes(String profile, boolean profileCheck, Instant since, String mid, Order order, Integer max, Deletes deletes) {
        return changes(profile, profileCheck, since, mid, order, max, deletes, Tail.IF_EMPTY);
    }

    public CountedIterator<MediaChange> changes(String profile, Instant since) {
        return changes(changesParameters().profile(profile).since(since).build());
    }

    public CountedIterator<MediaChange> changes(Instant since) {
        return changes(MediaSince.of(since));
    }

    public CountedIterator<MediaChange> changes(MediaSince since) {
        return changes(changesParameters().mediaSince(since));
    }

    public CountedIterator<MediaChange> changes(String profile, boolean profileCheck, Instant since, String mid, Order order, Integer max, Deletes deletes, Tail tail) {
        return changes(changesParameters().profile(profile).profileCheck(profileCheck).since(since).mid(mid).order(order).max(max).deletes(deletes).tail(tail));
    }

    @Getter
    @Setter
    private Duration  waitBetweenChangeListening = Duration.ofSeconds(2);

    public Future<Instant> subscribeToChanges(String profile, Instant since, BooleanSupplier until, final Consumer<MediaChange> listener) {
        return subscribeToChanges(profile, since, Deletes.ID_ONLY, until, listener);
    }

    public Future<Instant> subscribeToChanges(Instant since, BooleanSupplier until, final Consumer<MediaChange> listener) {
        return subscribeToChanges(null, since, until, listener);
    }

     public Future<Instant> subscribeToChanges(
         @Nullable String profile,
         final Instant initialSince,
         Deletes deletes,
         BooleanSupplier doWhile,
         final Consumer<MediaChange> listener) {
         return subscribeToChanges(
             ChangesFeedParameters.changesParameters()
                 .profile(profile)
                 .since(initialSince)
                 .deletes(deletes)
                 .build(),
             doWhile,
             Consumers.ignoreArg1(listener)
         ).thenApply(MediaSince::getInstant);

     }

    public CompletableFuture<MediaSince> subscribeToChanges(
        final ChangesFeedParameters parameters,
        BooleanSupplier doWhile,
        final BiConsumer<MediaSince, MediaChange> listener) {
        if (doWhile.getAsBoolean()) {
            return CompletableFuture.supplyAsync(() -> {
                ChangesFeedParameters effectiveParameters = parameters;
                MediaSince currentSince = parameters.getMediaSince();

                while (doWhile.getAsBoolean() && !Thread.currentThread().isInterrupted()) {
                    try (CountedIterator<MediaChange> changes = changes(effectiveParameters)) {
                        while (changes.hasNext()) {
                            MediaChange change = changes.next();
                            currentSince = change.asSince();
                            listener.accept(currentSince, change);
                            effectiveParameters = parameters.withMediaSince(currentSince);
                        }
                    } catch (NullPointerException npe) {
                        log.error(npe.getClass().getSimpleName(), npe);
                    } catch (ConnectException ce) {
                        if (loggedAboutConnect.isBefore(Instant.now().minus(Duration.ofMinutes(5)))) {
                            log.info(ce.getClass() + ":" + ce.getMessage());
                            loggedAboutConnect = Instant.now();
                        } else {
                            log.debug(ce.getClass() + ":" + ce.getMessage());
                        }

                    } catch (Exception e) {
                        log.info(e.getClass() + ":" + e.getMessage());
                    }
                    try {
                        synchronized (listener) {
                            listener.wait(waitBetweenChangeListening.toMillis());
                        }
                    } catch (InterruptedException iae) {
                        log.info("Interrupted");
                        Thread.currentThread().interrupt();
                    }
                }
                log.info("Ready listening for changes until: {}, interrupted: {}", doWhile.getAsBoolean(), Thread.currentThread().isInterrupted());
                synchronized (listener) {
                    listener.notifyAll();
                }
                return currentSince;
            }, EXECUTOR_SERVICE);
        } else {
            log.info("No started changes listening, because doWhile condition is already false");
            return CompletableFuture.completedFuture(parameters.getMediaSince());
        }

    }


    public RedirectList redirects() {
        try (Response response = clients.getMediaService().redirects(null)) {
            return response.readEntity(RedirectList.class);
        }

    }

    public JsonArrayIterator<MediaChange> changes(ChangesFeedParameters.Builder parameters)  {
        return changes(parameters.build());
    }


    public JsonArrayIterator<MediaChange> changes(ChangesFeedParameters parameters)  {
        limiter.acquire();
        try {
            try {
                JsonArrayIterator<MediaChange> result = MediaRestClientUtils.changes(clients.getMediaServiceNoTimeout(), parameters);
                limiter.upRate();
                return result;
            } catch (ConnectException ce) {
                throw ce;
            }
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
    public <T extends MediaObject> T findByMid(boolean loadDeleted, String mid) {
        try {
            if (loadDeleted) {
                throw new UnsupportedOperationException();
            }
            return (T) load(mid)[0];
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * It is (currently?) not possible to load deleted objects from the frontend api, so
     * this defaults to <code>findByMid(false, mid)}</code>
     */
    @Override
    public  <T extends MediaObject> T  findByMid(String mid) {
        return findByMid(false, mid);
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

    public boolean isAvailable() {
        return clients.isAvailable();
    }
}
