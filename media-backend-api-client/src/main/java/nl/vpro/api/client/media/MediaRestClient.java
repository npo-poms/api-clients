package nl.vpro.api.client.media;

import lombok.*;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Response;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.BasicAuthentication;
import org.meeuw.functional.TriFunction;

import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.RateLimiter;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import nl.vpro.api.client.resteasy.AbstractApiClient;
import nl.vpro.api.rs.subtitles.*;
import nl.vpro.domain.media.*;
import nl.vpro.domain.media.search.*;
import nl.vpro.domain.media.support.OwnerType;
import nl.vpro.domain.media.update.*;
import nl.vpro.domain.media.update.collections.XmlCollection;
import nl.vpro.domain.subtitles.Subtitles;
import nl.vpro.domain.subtitles.SubtitlesId;
import nl.vpro.logging.simple.Level;
import nl.vpro.poms.shared.Headers;
import nl.vpro.rs.VersionRestService;
import nl.vpro.rs.client.VersionResult;
import nl.vpro.rs.media.FrameCreatorRestService;
import nl.vpro.rs.media.MediaBackendRestService;
import nl.vpro.util.*;

import static nl.vpro.domain.media.EntityType.AllMedia.valueOf;
import static nl.vpro.domain.media.search.Pager.Direction.ASC;

/**
 * A client for RESTfull calls to a running MediaBackendRestService.
 * <p>
 * This wraps a proxy which automatically implementing the interface {@link MediaBackendRestService} (accessible via {@link #getBackendRestService()})
 * <p>
 * Several utilities are provided (like {@link #get(String)}
 * ${@link #set(MediaUpdate)}). All raw calls can be done via
 * {@link #getBackendRestService()}
 *
 * The raw calls have more arguments which you may not always want to set. In
 * future version arguments can be added. If in these 'raw' calls leave
 * arguments <code>null</code> which are also set in the client (like 'errors'),
 * then they will be automaticly filled (the MediaBackendInterface is proxied
 * (with {@link MediaRestClientAspect}) to make this possible)
 * <p>
 * Also, this client can implicitly throttle itself. Calls like this are rated
 * on the POMS side, and like this you can avoid using it up too quickly.
 * <p>
 * Use it like this:
 *
 * <pre>
 * protected MediaRestClient getClient(Map<String, Object> headers) {
 * MeidaRestClient client = new MediaRestClient();
 * client.setUrl(mediaRsUrl);
 * client.setUserName(userName);
 * client.setPassword(password);
 * client.setErrors(getMail());
 * client.setHeaders(headers);
 * return client;
 * }
 * private void send(ProgramUpdate update) {
 * MediaRest client = getClient(getHeaders());
 * client.set(update);
 * }
 **
 * </pre>
 *
 * You can also configure it implicitly: MediaRestClient client = new MediaRestClient().configured();
 *
 * @author Michiel Meeuwissen
 */

@SuppressWarnings({"WeakerAccess", "UnnecessaryLocalVariable", "UnstableApiUsage"})
public class MediaRestClient extends AbstractApiClient implements MediaRestClientMXBean {

    public static final TriFunction<Method, Object[], String, Level> DEFAULT_HEADER_LEVEL = (m, a, s) -> s.equals(Headers.NPO_WARNING_HEADER) ? Level.WARN : Level.DEBUG;

    private int defaultMax = 50;

    private final RateLimiter throttle = RateLimiter.create(1.0);
    private final RateLimiter asynchronousThrottle = RateLimiter.create(0.4);

    @Getter
    @Setter
    private boolean followMerges;

    private MediaBackendRestService proxy;

    private FrameCreatorRestService frameCreatorRestService;

    private Map<String, Object> headers;

    Supplier<VersionResult> version;

    protected String userName;
    protected String password;
    @Setter
    @Getter
    protected String errors;

    @Getter
    @Setter
    protected boolean waitForRetry = false;

    @Getter
    @Setter
    protected boolean lookupCrids = true;

    @Getter
    @Setter
    private boolean validateInput = false;


    @Getter
    @Setter
    private AssemblageConfig.Steal stealCrids = AssemblageConfig.Steal.IF_DELETED;


    @Getter
    @Setter
    private boolean imageMetaData = false;

    @Getter
    @Setter
    private OwnerType owner = OwnerType.BROADCASTER;

    @Getter
    @Setter
    private Queue<String> warnings = new ArrayDeque<>(100);

    @Getter
    @Setter
    private boolean publishImmediately = false;

    @Getter
    @Setter
    private Boolean deletes = null;

    @Getter
    @Setter
    private TriFunction<Method, Object[], String, Level> headerLevel = DEFAULT_HEADER_LEVEL;


    Set<String> roles = new HashSet<>();


    @CanIgnoreReturnValue
    public <T> T doValidated(Callable<T> callable) throws Exception {
        boolean was = validateInput;
        try {
            validateInput = true;
            return callable.call();
        } finally {
            validateInput = was;
        }

    }

    public void doValidated(Runnable runnable) throws Exception {
        doValidated(() -> {
            runnable.run();
            return null;
        });
    }

    public static class Builder {

    }

    @SuppressWarnings({"SpringAutowiredFieldsWarningInspection", "OptionalUsedAsFieldOrParameterType", "unused"})
    @Named
    public static class Provider implements javax.inject.Provider<MediaRestClient> {

        @Inject
        @Named("npo-media_api_backend.baseUrl")
        String baseUrl;
        @Inject
        @Named("npo-media_api_backend.user")
        String user;
        @Inject
        @Named("npo-media_api_backend.password")
        String password;
        @Inject
        @Named("npo-media_api_backend.errors")
        Optional<String> errors;
        @Inject
        @Named("npo-media_api_backend.trustAll")
        Optional<Boolean> trustAll;


        @Inject
        @Named("npo-media_api_backend.connectionInPoolTTL")
        Optional<String> connectionInPoolTTL;

        @Inject
        @Named("npo-media_api_backend.validateAfterInactivity")
        Optional<String> validateAfterInactivity;

        @Override
        public MediaRestClient get() {
            return ProviderAndBuilder.fillAndCatch(this, builder()).build();
        }
    }

    @lombok.Builder(builderClassName = "Builder")
    protected MediaRestClient(
        String baseUrl,
        Duration connectionRequestTimeout,
        Duration connectTimeout,
        Duration socketTimeout,
        Integer maxConnections,
        Integer maxConnectionsPerRoute,
        Integer maxConnectionsNoTimeout,
        Integer maxConnectionsPerRouteNoTimeout,
        Duration connectionInPoolTTL,
        Duration validateAfterInactivity,
        Duration countWindow,
        Integer bucketCount,
        Duration warnThreshold,
        nl.vpro.logging.simple.Level warnLevel,
        List<Locale> acceptableLanguages,
        Boolean trustAll,
        Integer defaultMax,
        boolean followMerges,
        Map<String, Object> headers,
        String userName,
        String password,
        String user,
        String errors,
        boolean waitForRetry,
        boolean lookupCrids,
        AssemblageConfig.Steal stealCrids,
        OwnerType owner,
        Double throttleRate,
        Double asynchronousThrottleRate,
        boolean validateInput,
        String mbeanName,
        ClassLoader classLoader,
        String userAgent,
        Boolean registerMBean,
        boolean publishImmediately,
        Boolean deletes,
        TriFunction<Method, Object[], String, Level> headerLevel,
        boolean eager) {
        super(
            baseUrl,
            connectionRequestTimeout,
            connectTimeout,
            socketTimeout,
            maxConnections,
            maxConnectionsPerRoute,
            maxConnectionsNoTimeout,
            maxConnectionsPerRouteNoTimeout,
            connectionInPoolTTL,
            validateAfterInactivity,
            countWindow,
            bucketCount,
            warnThreshold,
            warnLevel,
            acceptableLanguages,
            null,
            null,
            trustAll,
            null, // only xml is relevant
            mbeanName,
            classLoader,
            userAgent,
            registerMBean,
            eager);
        if (defaultMax != null) {
            this.defaultMax = defaultMax;
        }
        this.followMerges = followMerges;
        this.headers = headers;
        if (user != null) {
            setUserNamePassword(user);
        }

        if (userName != null) {
            if (userName.contains(":")) {
                log.info("User seem to be configured with password");
                setUserNamePassword(userName);
            } else {
                this.userName = userName;
            }
        }
        if (password != null) {
            this.password = password;
        }
        this.errors = errors;
        this.waitForRetry = waitForRetry;
        this.lookupCrids = lookupCrids;
        this.stealCrids = stealCrids;
        if (throttleRate != null) {
            this.setThrottleRate(throttleRate);
        }
        if (asynchronousThrottleRate != null) {
            this.setAsynchronousThrottleRate(asynchronousThrottleRate);
        }
        this.validateInput = validateInput;
        this.owner = owner;
        this.publishImmediately = publishImmediately;
        this.deletes = deletes;
        this.headerLevel = headerLevel == null ? DEFAULT_HEADER_LEVEL : headerLevel;
    }


    @Override
    protected Stream<Supplier<?>> services() {
        return Stream.of(
            this::getBackendRestService,
            this::getFrameCreatorRestService
        );
    }


    public static Builder configured(Env env, String... configFiles) {
        Builder builder = builder();
        ConfigUtils.configured(env, builder, configFiles);
        return builder;
    }

    /**
     * Read configuration from a config file in ${user.home}/conf/mediarestclient.properties
     */
    public static Builder configured(Env env) {
        Builder builder = builder();
        ConfigUtils.configuredInHome(env, builder, "mediarestclient.properties", "creds.properties");
        return builder;
    }

    public static Builder configured(Env env, Map<String, String> settings) {
        Builder builder = builder();
        ConfigUtils.configured(env, builder, settings);
        return builder;
    }

    public static Builder configured() {
        return configured(null);
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public void setUserName(String userName) {
        this.userName = userName;
        invalidate();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
        invalidate();
    }

    public void setUserNamePassword(String semicolonSeperated) {
        String[] userNamePassword = semicolonSeperated.split(":", 2);
        setUserName(userNamePassword[0]);
        if (userNamePassword.length == 2) {
            setPassword(userNamePassword[1]);
        }
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public String getVersion() {
        if (version == null) {
            version = () -> Suppliers.memoizeWithExpiration(() -> {
                try {
                    VersionRestService p = proxyErrorsAndCount(VersionRestService.class,
                            getTarget(getClientHttpEngine())
                                .proxy(VersionRestService.class));
                    String v = p.getVersion();
                    if (v != null) {
                        return VersionResult.builder().version(v).available(true).build();
                    }
                } catch (javax.ws.rs.NotFoundException | ServiceUnavailableException nfe) {
                    return VersionResult.builder().version("5.11.6").available(true).build();
                } catch (Exception io) {
                    log.warn(io.getClass().getName() + " " + io.getMessage());
                }
                return VersionResult.builder().version("unknown").available(false).build();
            }, 30L, TimeUnit.MINUTES).get();
        }
        return version.get().getVersion();
    }

    public boolean isAvailable() {
        getVersion();
        return version.get().isAvailable();
    }

    /**
     * The version of the rest-service we are talking too.
     *
     * @return An object representing the major/minor version.
     */
    public IntegerVersion getVersionNumber() {
        String version = getVersion();
        return Version.parseIntegers(version);
    }

    @Override
    protected void buildResteasy(ResteasyClientBuilder builder) {
        if (userName == null || password == null) {
            throw new IllegalStateException(
                    "User name (" + userName + ") and password (" + password + ") should both be non null");
        }

        builder.httpEngine(getClientHttpEngine())
            .register(new BasicAuthentication(userName, password))
            .register(new AddRequestHeadersFilter())
            .register(VTTSubtitlesReader.class)
            .register(EBUSubtitlesReader.class)
            .register(SRTSubtitlesReader.class)
            .register(VTTWriter.class)
            .register(VTTSubtitlesWriter.class)
            .register(ContentTypeInterceptor.class)
        ;
    }


    /**
     * returns the proxied interface as is actually used on the POMS backend. This
     * is (as long as your client's version corresponds) guaranteed to be complete
     * and correct.
     */
    public MediaBackendRestService getBackendRestService() {
        if (proxy == null) {
            log.info("Creating proxy for {} {}@{}", MediaBackendRestService.class, userName, baseUrl);
            ResteasyWebTarget target = getTarget(getClientHttpEngine());
            //target.setChunked(true);
            proxy = MediaRestClientAspect.proxy(this,
                proxyErrorsAndCount(
                    MediaBackendRestService.class,
                    target.proxy(MediaBackendRestService.class),
                    nl.vpro.rs.Error.class
                ),
                MediaBackendRestService.class
            );
        }
        return proxy;
    }

    public FrameCreatorRestService getFrameCreatorRestService() {
        if (frameCreatorRestService == null) {
            frameCreatorRestService = MediaRestClientAspect.proxy(this,
                proxyErrorsAndCount(FrameCreatorRestService.class,
                    getTarget(getClientHttpEngine()).proxy(FrameCreatorRestService.class)),
                FrameCreatorRestService.class);
        }
        return frameCreatorRestService;
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    protected <T extends MediaUpdate<?>> T get(final @NonNull Class<T> type, final @NonNull String id) {
        int retryCountDown = 20;
        while(true) {
            try {
                return (T) getBackendRestService()
                    .getMedia(valueOf(type), id, followMerges, owner);
            } catch (Exception noHttpResponseException) {
                retryAfterWaitOrException(noHttpResponseException.getMessage(), retryCountDown--);
            }
        }
    }


    @SuppressWarnings("unchecked")
    @SneakyThrows
    @Nullable
    protected <T extends MediaObject> T getFull(final @NonNull Class<T> type, final @NonNull String id) {
        return (T) getBackendRestService().getFullMediaObject(valueOf(type), id, followMerges);
    }

    /**
     * Returns the program (as an 'update' object'), with the given id. Or
     * <code>null</code> if not found.
     *
     * @param id This can be an URN, MID, or crid.
     */
    @Nullable
    public ProgramUpdate getProgram(String id) {
        return get(ProgramUpdate.class, id);
    }

    @Nullable
    public SegmentUpdate getSegment(String id) {
        return get(SegmentUpdate.class, id);
    }

    @SuppressWarnings("unchecked")
    public <T extends MediaUpdate<?>> T get(String id) {
        return (T) get(MediaUpdate.class, id);
    }


    @SuppressWarnings("unchecked")
    public <T extends MediaUpdate<?>> Optional<T> optional(String id) {
        try {
            return Optional.ofNullable((T) get(MediaUpdate.class, id));
        } catch (ResponseError re) {
            if (re.getStatus() == 404) {
                return Optional.empty();
            }
            throw re;
        }
    }

    @SneakyThrows
    @NonNull
    public String delete(@NonNull String mid) {
        try (Response response = getBackendRestService().deleteMedia(null, mid, followMerges, errors)) {
            String result = response.readEntity(String.class);
            return result;
        }
    }

    public String deleteIfExists(String mid) {
        try {
            return delete(mid);
        } catch (ResponseError responseError) {
            if (responseError.getStatus() == 404) {
                return responseError.getMessage();
            }
            throw responseError;
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public String addImage(ImageUpdate update, String mid) {
        try (Response response = getBackendRestService().addImage(update, null, mid, followMerges, errors, validateInput, imageMetaData, owner, publishImmediately)) {
            String result = response.readEntity(String.class);
            return result;
        }
    }

    @SneakyThrows
    public SortedSet<LocationUpdate> cloneLocations(String id) {

        SortedSet<LocationUpdate> result = new TreeSet<>();
        try {
            XmlCollection<LocationUpdate> i = getBackendRestService().getLocations(EntityType.NoGroups.media, id, true, owner);
            for (LocationUpdate lu : i) {
                lu.setUrn(null);
                result.add(lu);
            }
        } catch (NullPointerException npe) {
            // dammit
        }
        return result;
    }

    /** add a location to a Program, Segment or Group */
    protected void addLocation(final EntityType.NoGroups type, final LocationUpdate location, final String id) {
        try (Response response = getBackendRestService()
            .addLocation(type, location, id, followMerges, errors, validateInput, owner)) {
            log.debug("{}", response);
        }
    }

    public void addLocationToProgram(LocationUpdate location, String programId) {
        addLocation(EntityType.NoGroups.program, location, programId);
    }

    public void addLocationToSegment(LocationUpdate location, String segmentId) {
        addLocation(EntityType.NoGroups.segment, location, segmentId);
    }


    @SneakyThrows
    public void createMember(String owner, String member, Integer number) {
        try (Response response = getBackendRestService().addMemberOf(
                new MemberRefUpdate(number, owner), EntityType.AllMedia.media, member, followMerges, errors, validateInput)) {
            log.debug("{}", response);
        }
    }

    @SneakyThrows
    public void removeMember(String owner, String member, Integer number) {
        try (Response response = getBackendRestService()
                .removeMemberOf(EntityType.AllMedia.media, member, owner, number, followMerges, errors)) {
            log.debug("{}", response);
        }
    }

    @SneakyThrows
    public void createEpisode(String owner, String member, Integer number) {
        try (Response response = getBackendRestService()
                .addEpisodeOf(new MemberRefUpdate(number, owner), member, followMerges, errors, validateInput)) {
            log.debug("{}", response);
        }
    }

    @SneakyThrows
    public void removeEpisode(String owner, String member, Integer number) {
        try (Response response = getBackendRestService().removeEpisodeOf(member, owner, number, followMerges, errors)) {
            log.debug("{}", response);
        }
    }

    public String transcode(TranscodeRequest request) {
        try (Response response = getBackendRestService().transcode(null,null,  request)) {
            String result = response.readEntity(String.class);
            return  result;
        }
    }

    protected String set(final EntityType type, final MediaUpdate<?> update) {
        return set(type, update, null);
    }

    @SneakyThrows
    protected String set(final EntityType type, final MediaUpdate<?> update, String errors) {
        if (errors == null) {
            errors = this.errors;
        }
        try (Response response = getBackendRestService().update(
            EntityType.AllMedia.valueOf(type.name()), update, followMerges, errors,
                    lookupCrids, stealCrids, validateInput, imageMetaData, owner, publishImmediately)) {
            String result = response.readEntity(String.class);
            return result;
        }
    }

    @SneakyThrows
    @CanIgnoreReturnValue
    public String removeSegment(String program, String segment) {
        try (Response response = getBackendRestService().removeSegment(program, segment, followMerges, errors)) {
            String result = response.readEntity(String.class);
            return result;
        }
    }

    protected boolean success(int statusCode) {
        return statusCode >= 200 && statusCode <= 299;
    }

    public String setProgram(ProgramUpdate program) {
        return set(EntityType.AllMedia.program, program);
    }

    public Program getFullProgram(String id) {
        return getFull(Program.class, id);
    }

    public GroupUpdate getGroup(String id) {
        return get(GroupUpdate.class, id);
    }

    public Group getFullGroup(String id) {
        return getFull(Group.class, id);
    }

    @SuppressWarnings("unchecked")
    public <T extends MediaObject> T getFull(String id) {
        return (T) getFull(MediaObject.class, id);
    }

    @SneakyThrows
    public MediaUpdateList<MemberUpdate> getGroupMembers(final String id, final int max, final long offset) {
        return getBackendRestService().getGroupMembers(EntityType.NoSegments.media, id, offset, max, ASC, followMerges, owner, deletes);
    }

    public MediaUpdateList<MemberUpdate> getGroupMembers(String id) {
        return getGroupMembers(id, defaultMax, 0);
    }

    @SneakyThrows
    public MediaUpdateList<MemberUpdate> getGroupEpisodes(final String id, final int max, final long offset) {
        return getBackendRestService().getGroupEpisodes(id, offset, max, ASC, followMerges, owner, deletes);
    }

    public MediaUpdateList<MemberUpdate> getGroupEpisodes(String id) {
        return getGroupEpisodes(id, defaultMax, 0);
    }

    public String setGroup(GroupUpdate group) {
        return set(EntityType.AllMedia.group, group);
    }

    public String setSegment(SegmentUpdate group) {
        return set(EntityType.AllMedia.segment, group);
    }

    public String set(MediaUpdate<?> mediaUpdate) {
        return set(EntityType.AllMedia.media, mediaUpdate);
    }

    public String set(MediaUpdate<?> mediaUpdate, String errors) {
        return set(EntityType.AllMedia.media, mediaUpdate, errors);
    }

    public Iterator<MemberUpdate> getAllMembers(String mid) {
        return BatchedReceiver.<MemberUpdate>builder()
            .batchSize(240)
            .batchGetter((offset, max) -> {
                return getBackendRestService().getGroupMembers(EntityType.NoSegments.media, mid, offset, max, ASC, followMerges, owner, deletes).iterator();
            })
            .build();
    }

    public Iterator<MemberUpdate> getAllEpisodes(String mid) {
        return BatchedReceiver.<MemberUpdate>builder()
            .batchSize(defaultMax)
            .batchGetter((offset, max) -> {
                return getBackendRestService().getGroupEpisodes(mid, offset, max, ASC, followMerges, owner, deletes).iterator();
            })
            .build();
    }


    public Iterator<Member> getAllFullMembers(String mid) {
        return BatchedReceiver.<Member>builder()
            .batchSize(defaultMax)
            .batchGetter((offset, max) -> {
                return getBackendRestService().getFullGroupMembers(EntityType.NoSegments.media, mid, offset, max, ASC, followMerges, deletes).iterator();
            })
            .build();
    }

    public Iterator<Member> getAllFullEpisodes(String mid) {
        return BatchedReceiver.<Member>builder()
            .batchSize(defaultMax)
            .batchGetter((offset, max) -> {
                return getBackendRestService().getFullGroupEpisodes(mid, offset, max, ASC, followMerges, deletes).iterator();
            })
            .build();
    }

    @SneakyThrows
    public MediaList<MediaListItem> find(MediaForm form) {
        return getBackendRestService().find(form, false, validateInput);
    }

    public String setSubtitles(Subtitles subtitles) {
        SubtitlesId id = subtitles.getId();
        try (Response response = getBackendRestService().setSubtitles(id.getMid(), id.getLanguage(), id.getType(), Duration.ZERO, true, errors, subtitles)) {
            String result = response.readEntity(String.class);
            log.debug("{}", result);
            return result;

        }
    }


    public String deleteSubtitles(SubtitlesId id) {
        try(Response response = getBackendRestService()
            .deleteSubtitles(id.getMid(), id.getLanguage(), id.getType(), true, errors)) {
            String result = response.readEntity(String.class);
            log.debug("{}", result);
            return result;
        }
    }

    public void setDefaultMax(int max) {
        this.defaultMax = max;
    }


    public double getThrottleRate() {
        return this.throttle.getRate();
    }

    /**
     * Sets the number of requests per seconds
     */
    public void setThrottleRate(double rate) {
        this.throttle.setRate(rate);
    }

    public double getAsynchronousThrottleCount() {
        return asynchronousThrottle.getRate();
    }

    public void setAsynchronousThrottleRate(double rate) {
        this.asynchronousThrottle.setRate(rate);
    }

    public Set<String> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    @Override
    public synchronized void invalidate() {
        super.invalidate();
        proxy = null;
    }

    @Override
    public String toString() {
        return userName + "@" + baseUrl;
    }

    void retryAfterWaitOrException(String action, RuntimeException e) {
        if (!waitForRetry) {
            throw e;
        }
        retryAfterWaitOrException(action + ":" + e.getMessage(), 1);
    }

    void retryAfterWaitOrException(String cause, int retryCount) {
        if (!waitForRetry || retryCount <= 0) {
            throw new RuntimeException(cause);
        }
        try {
            log.warn(userName + "@" + baseUrl + " " + cause + ", retrying after 30 s");
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    void throttle() {
        throttle.acquire();
    }

    private void throttleAsynchronous() {
        asynchronousThrottle.acquire();
    }

    public class AddRequestHeadersFilter implements ClientRequestFilter {

        public AddRequestHeadersFilter() {
        }

        @Override
        public void filter(ClientRequestContext requestContext) {
            if (headers != null) {
                for (Map.Entry<String, Object> e : headers.entrySet()) {
                    requestContext.getHeaders().add(e.getKey(), e.getValue());
                }
            }
        }
    }
}
