package nl.vpro.rs.media;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.RateLimiter;

import nl.vpro.api.client.resteasy.AbstractApiClient;
import nl.vpro.api.client.utils.VersionResult;
import nl.vpro.api.rs.subtitles.*;
import nl.vpro.domain.media.*;
import nl.vpro.domain.media.search.MediaForm;
import nl.vpro.domain.media.search.MediaList;
import nl.vpro.domain.media.search.MediaListItem;
import nl.vpro.domain.media.support.OwnerType;
import nl.vpro.domain.media.update.*;
import nl.vpro.domain.media.update.collections.XmlCollection;
import nl.vpro.domain.subtitles.Subtitles;
import nl.vpro.domain.subtitles.SubtitlesId;
import nl.vpro.rs.VersionRestService;
import nl.vpro.util.*;

/**
 * A client for RESTful calls to a running MediaBackendRestService.
 *
 * Several utilities are provided (like {@link #get(String)}
 * ${@link #set(MediaUpdate)}). All raw calls can be done via
 * {@link #getBackendRestService()}
 *
 * The raw calls have more arguments which you may not always want to set. In
 * future version arguments can be added. If in these 'raw' calls leave
 * arguments <code>null</code> which are also set in the client (like 'errors'),
 * then they will be automaticly filled (the MediaBackendInterface is proxied
 * (with {@link MediaRestClientAspect}) to make this possible)
 *
 * Also this client can implicitely throttle itself. Calls like this are rated
 * on the POMS side, and like this you can avoid using it up too quickly.
 *
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
 * You can also configured it implicitly: MediaRestClient client = new MediaRestClient().configured();
 *
 * @author Michiel Meeuwissen
 */

public class MediaRestClient extends AbstractApiClient implements MediaRestClientMXBean {

    private int defaultMax = 50;

    private final RateLimiter throttle = RateLimiter.create(1.0);
    private final RateLimiter asynchronousThrottle = RateLimiter.create(0.4);

    @Getter
    @Setter
    private boolean followMerges = true;

    private MediaBackendRestService proxy;

    private FrameCreatorRestService frameCreatorRestService;

    private Map<String, Object> headers;

    Supplier<VersionResult> version;

    protected String userName;
    protected String password;
    @Setter
    @Getter
    protected String errors;
    @lombok.Builder.Default
    protected boolean waitForRetry = false;

    @lombok.Builder.Default
    @Getter
    @Setter
    protected boolean lookupCrids = true;

    @lombok.Builder.Default
    @Getter
    @Setter
    private boolean validateInput = false;

    @lombok.Builder.Default
    @Getter
    @Setter
    private boolean imageMetaData = false;

    @lombok.Builder.Default
    @Getter
    @Setter
    private OwnerType owner = OwnerType.BROADCASTER;



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

    @SuppressWarnings({ "SpringAutowiredFieldsWarningInspection", "OptionalUsedAsFieldOrParameterType" })
    @Named
    public static class Provider implements javax.inject.Provider<MediaRestClient> {

        @Inject
        @Named("npo-mediabackend-api.baseUrl")
        String baseUrl;
        @Inject
        @Named("npo-mediabackend-api.user")
        String user;
        @Inject
        @Named("npo-mediabackend-api.password")
        String password;
        @Inject
        @Named("npo-mediabackend-api.errors")
        Optional<String> errors;
        @Inject
        @Named("npo-mediabackend-api.trustAll")
        Optional<Boolean> trustAll;

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
        Duration countWindow,
        Integer bucketCount,
        Duration warnThreshold,
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
        OwnerType owner,
        Double throttleRate,
        Double asynchronousThrottleRate,
        boolean validateInput,
        String mbeanName,
        ClassLoader classLoader,
        String userAgent) {
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
            countWindow,
            bucketCount,
            warnThreshold,
            acceptableLanguages,
            null,
            null,
            trustAll,
            null, // only xml is relevant
            mbeanName,
            classLoader,
            userAgent);
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
        if (throttleRate != null) {
            this.setThrottleRate(throttleRate);
        }
        if (asynchronousThrottleRate != null) {
            this.setAsynchronousThrottleRate(asynchronousThrottleRate);
        }
        this.validateInput = validateInput;
        this.owner = owner;
    }

    enum Type {
        SEGMENT,
        PROGRAM,
        GROUP,
        MEDIA
        ;

        private static Type valueOf(Class type) {
            if (Program.class.isAssignableFrom(type)) return PROGRAM;
            if (ProgramUpdate.class.isAssignableFrom(type)) return PROGRAM;
            if (Group.class.isAssignableFrom(type)) return GROUP;
            if (GroupUpdate.class.isAssignableFrom(type)) return GROUP;
            if (Segment.class.isAssignableFrom(type)) return SEGMENT;
            if (SegmentUpdate.class.isAssignableFrom(type)) return SEGMENT;
            if (MediaObject.class.isAssignableFrom(type)) return MEDIA;
            if (MediaUpdate.class.isAssignableFrom(type)) return MEDIA;
            throw new IllegalArgumentException();
        }

        public String toString() {
            return super.toString().toLowerCase();
        }
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

    public boolean isWaitForRetry() {
        return waitForRetry;
    }

    public void setWaitForRetry(boolean waitForRetry) {
        this.waitForRetry = waitForRetry;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public String getVersion() {
        if (version == null) {
            version = () -> Suppliers.memoizeWithExpiration(() -> {
                try {
                    VersionRestService p = proxyErrorsAndCount(VersionRestService.class,
                            getTarget(getClientHttpEngine()).proxy(VersionRestService.class));
                    String v = p.getVersion();
                    if (v != null) {
                        return VersionResult.builder().version(v).available(true).build();
                    }
                } catch (javax.ws.rs.NotFoundException nfe) {
                    return VersionResult.builder().version("4.8.6").available(true).build();
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
     * @return a float representing the major/minor version. The patch level is
     *         added as thousands.
     */
    public Float getVersionNumber() {
        try {
            String version = getVersion();
            Matcher matcher = Pattern.compile("(\\d+\\.\\d+)\\.?(\\d+)?.*").matcher(version);
            if (matcher.matches()) {
                Double result = Double.parseDouble(matcher.group(1));
                String minor = matcher.group(2);
                if (minor != null) {
                    result += (double) Integer.parseInt(minor) / 1000d;
                }
                return result.floatValue();
            }
        } catch (NumberFormatException ignored) {
        }
        return 0f;
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
     * is (as long as your client's version corresponds) garantueed to be complete
     * and correct.
     */
    public MediaBackendRestService getBackendRestService() {
        if (proxy == null) {
            log.info("Creating proxy for {} {}@{}", MediaBackendRestService.class, userName, baseUrl);
            proxy = MediaRestClientAspect.proxy(this,
                proxyErrorsAndCount(MediaBackendRestService.class,
                    getTarget(getClientHttpEngine()).proxy(MediaBackendRestService.class)),
                MediaBackendRestService.class);
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
    protected <T extends MediaUpdate<?>> T get(final Class<T> type, final String id) {
        try {
            return (T) getBackendRestService()
                .getMedia(Type.valueOf(type).toString(), id, followMerges, owner);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T extends MediaObject> T getFull(final Class<T> type, final String id) {
        try {
            return (T) getBackendRestService().getFullMediaObject(Type.valueOf(type).toString(), id, followMerges);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the program (as an 'update' object'), with the given id. Or
     * <code>null</code> if not found.
     *
     * @param id This can be an URN, MID, or crid.
     */
    public ProgramUpdate getProgram(String id) {
        return get(ProgramUpdate.class, id);
    }

    public SegmentUpdate getSegment(String id) {
        return get(SegmentUpdate.class, id);
    }

    @SuppressWarnings("unchecked")
    public <T extends MediaUpdate<?>> T get(String id) {
        return (T) get(MediaUpdate.class, id);
    }

    public String delete(String mid) {
        try {
            Response response = getBackendRestService().deleteMedia(null, mid, followMerges, errors);
            String result = response.readEntity(String.class);
            response.close();
            return result;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String addImage(ImageUpdate update, String mid) {
        Response response = getBackendRestService().addImage(update, null, mid, followMerges, errors, validateInput, imageMetaData);
        String result = response.readEntity(String.class);
        response.close();
        return result;
    }

    public SortedSet<LocationUpdate> cloneLocations(String id) {

        SortedSet<LocationUpdate> result = new TreeSet<>();
        try {
            XmlCollection<LocationUpdate> i = getBackendRestService().getLocations("media", id, true);
            for (LocationUpdate lu : i) {
                lu.setUrn(null);
                result.add(lu);
            }
        } catch (NullPointerException npe) {
            // dammit
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /** add a location to a Program, Segment or Group */
    protected void addLocation(final Type type, final LocationUpdate location, final String id) {
        Response response = getBackendRestService()
            .addLocation(type.toString(), location, id, followMerges, errors, validateInput);
        response.close();
    }

    public void addLocationToProgram(LocationUpdate location, String programId) {
        addLocation(Type.PROGRAM, location, programId);
    }

    public void addLocationToSegment(LocationUpdate location, String segmentId) {
        addLocation(Type.SEGMENT, location, segmentId);
    }

    public void addLocationToGroup(LocationUpdate location, String groupId) {
        addLocation(Type.GROUP, location, groupId);
    }

    public void createMember(String owner, String member, Integer number) {
        try {
            Response response = getBackendRestService().addMemberOf(
                new MemberRefUpdate(number, owner), "media", member, followMerges, errors, validateInput);
            response.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeMember(String owner, String member, Integer number) {
        try {
            Response response = getBackendRestService()
                .removeMemberOf("media", member, owner, number, followMerges, errors);
            response.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void createEpisode(String owner, String member, Integer number) {
        try {
            Response response = getBackendRestService()
                .addEpisodeOf(new MemberRefUpdate(number, owner), member, followMerges, errors, validateInput);
            response.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeEpisode(String owner, String member, Integer number) {
        try {
            Response response = getBackendRestService().removeEpisodeOf(member, owner, number, followMerges, errors);
            response.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String transcode(TranscodeRequest request) {
        Response response = getBackendRestService().transcode(null, request);
        String result = response.readEntity(String.class);
        response.close();
        return  result;
    }

    protected String set(final Type type, final MediaUpdate update) {
        return set(type, update, null);
    }

    protected String set(final Type type, final MediaUpdate update, String errors) {
        if (errors == null) {
            errors = this.errors;
        }
        try {
            Response response = getBackendRestService().update(type.toString(), update, followMerges, errors,
                    lookupCrids, validateInput, imageMetaData, owner);
            String result = response.readEntity(String.class);
            response.close();
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String removeSegment(String program, String segment) {
        try {
            Response response = getBackendRestService().removeSegment(program, segment, followMerges, errors);
            String result = response.readEntity(String.class);
            response.close();
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean success(int statusCode) {
        return statusCode >= 200 && statusCode <= 299;
    }

    public String setProgram(ProgramUpdate program) {
        return set(Type.PROGRAM, program);
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

    public MediaUpdateList<MemberUpdate> getGroupMembers(final String id, final int max, final long offset) {
        try {
            return getBackendRestService().getGroupMembers("media", id, offset, max, "ASC", followMerges);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MediaUpdateList<MemberUpdate> getGroupMembers(String id) {
        return getGroupMembers(id, defaultMax, 0);
    }

    public MediaUpdateList<MemberUpdate> getGroupEpisodes(final String id, final int max, final long offset) {
        try {
            return getBackendRestService().getGroupEpisodes(id, offset, max, "ASC", followMerges);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MediaUpdateList<MemberUpdate> getGroupEpisodes(String id) {
        return getGroupEpisodes(id, defaultMax, 0);
    }

    public String setGroup(GroupUpdate group) {
        return set(Type.GROUP, group);
    }

    public String setSegment(SegmentUpdate group) {
        return set(Type.SEGMENT, group);
    }

    public String set(MediaUpdate mediaUpdate) {
        return set(Type.MEDIA, mediaUpdate);
    }

    public String set(MediaUpdate mediaUpdate, String errors) {
        return set(Type.MEDIA, mediaUpdate, errors);
    }

    public Iterator<MemberUpdate> getAllMembers(String mid) throws IOException {
        return BatchedReceiver.<MemberUpdate>builder()
            .batchSize(240)
            .batchGetter((offset, max) -> {
                try {
                    return getBackendRestService().getGroupMembers("media", mid, offset, max, "ASC", followMerges).iterator();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            })
            .build();
    }

    public Iterator<MemberUpdate> getAllEpisodes(String mid) throws IOException {
        return BatchedReceiver.<MemberUpdate>builder()
            .batchSize(defaultMax)
            .batchGetter((offset, max) -> {
                try {
                    return getBackendRestService().getGroupEpisodes(mid, offset, max, "ASC", followMerges).iterator();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            })
            .build();
    }


    public Iterator<Member> getAllFullMembers(String mid) throws IOException {
        return BatchedReceiver.<Member>builder()
            .batchSize(defaultMax)
            .batchGetter((offset, max) -> {
                try {
                    return getBackendRestService().getFullGroupMembers("media", mid, offset, max, "ASC", followMerges).iterator();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            })
            .build();
    }

    public Iterator<Member> getAllFullEpisodes(String mid) throws IOException {
        return BatchedReceiver.<Member>builder()
            .batchSize(defaultMax)
            .batchGetter((offset, max) -> {
                try {
                    return getBackendRestService().getFullGroupEpisodes(mid, offset, max, "ASC", followMerges).iterator();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            })
            .build();
    }

    public MediaList<MediaListItem> find(MediaForm form) {
        try {
            return getBackendRestService().find(form, false, validateInput);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSubtitles(Subtitles subtitles) {
        SubtitlesId id = subtitles.getId();
        Response response = getBackendRestService().setSubtitles(id.getMid(), id.getLanguage(), id.getType(), Duration.ZERO, true, errors, subtitles);
        response.close();
    }


    public void deleteSubtitles(SubtitlesId id) {
        Response response = getBackendRestService()
            .deleteSubtitles(id.getMid(), id.getLanguage(), id.getType(), true, errors);
        response.close();


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
        retryAfterWaitOrException(action + ":" + e.getMessage());
    }

    void retryAfterWaitOrException(String cause) {
        if (!waitForRetry) {
            throw new RuntimeException(cause);
        }
        try {
            log.warn(userName + "@" + baseUrl + " " + cause + ", retrying after 30 s");
            Thread.sleep(30000);
        } catch (InterruptedException e) {
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
        public void filter(ClientRequestContext requestContext) throws IOException {
            if (headers != null) {
                for (Map.Entry<String, Object> e : headers.entrySet()) {
                    requestContext.getHeaders().add(e.getKey(), e.getValue());
                }
            }
        }
    }
}
