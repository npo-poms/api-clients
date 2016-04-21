package nl.vpro.rs.media;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

import nl.vpro.api.client.resteasy.AbstractApiClient;
import nl.vpro.api.client.resteasy.ErrorAspect;
import nl.vpro.domain.media.Group;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.Program;
import nl.vpro.domain.media.Segment;
import nl.vpro.domain.media.search.MediaForm;
import nl.vpro.domain.media.search.MediaListItem;
import nl.vpro.domain.media.update.*;
import nl.vpro.domain.media.update.collections.XmlCollection;

/**
 * A client for RESTful calls to a running MediaBackendRestService.
 *
 * Several utilities are provided (like {@link #get(String)} ${@link #set(MediaUpdate}). All raw calls can be done via {@link #getBackendRestService()}
 *
 * The raw calls have more arguments which you may not always want to set. In future version arguments can be added.
 * If in these 'raw' calls leave arguments <code>null</code> which are also set in the client (like 'errors'), then they will be automaticly filled
 * (the MediaBackendInterface is proxied (with {@link MediaRestClientAspect}) to make this possible)
 *
 * Also this client can implicitely trottle itself. Calls like this are rated on the POMS side, and like this you can aovoid using it up too quickly.
 *
 * <p/>
 * Use it like this:
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
 * You can also configured it implicetely:
 * MediaRestClient client = new MediaRestClient().configured();
 *
 * @author Michiel Meeuwissen
 */
public class MediaRestClient extends AbstractApiClient {

    private static Logger LOG = LoggerFactory.getLogger(MediaRestClient.class);

    private int defaultMax = 50;

    private final RateLimiter throttle = RateLimiter.create(1.0);
    private final RateLimiter asynchronousThrottle = RateLimiter.create(0.4);

	private boolean followMerges = true;

    private MediaBackendRestService proxy;
    private Map<String, Object> headers;

    public MediaRestClient() {
        this(-1, 10, 2);
    }
    public MediaRestClient(int connectionTimeoutMillis, int maxConnections, int connectionInPoolTTL) {
        super(connectionTimeoutMillis, maxConnections, connectionInPoolTTL);
    }

    enum Type {
        SEGMENT,
        PROGRAM,
        GROUP,
        MEDIA;

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

    protected String userName;
    protected String password;
    protected String url = "https://api-dev.poms.omroep.nl";
    protected String errors;
    protected boolean waitForRetry = false;
	protected boolean lookupCrids = true;


    public MediaRestClient configured(String configFile) throws IOException {
        Properties properties = new Properties();
        File file = new File(configFile);
        if (! file.canRead()) {
            LOG.info("The file {} cannot be read", file);
        } else {
            LOG.info("Reading properties from {}", file);
            properties.load(new FileInputStream(file));
            properties.forEach(this::setProperty);
        }
        return this;
    }

    /**
     * Read configuration from a config file in ${user.home}/conf/mediarestclient.properties
     */
    public MediaRestClient configured() throws IOException {
        return configured(System.getProperty("user.home") + File.separator + "conf" + File.separator + "mediarestclient.properties");

    }
    protected void setProperty(Object key, Object value)  {
        Method[] methods = this.getClass().getMethods();
        String k = (String) key;
        String v = (String) value;
        String setter = "set" + Character.toUpperCase(k.charAt(0)) + k.substring(1);
        for (Method m : methods) {
            if (m.getName().equals(setter) && m.getParameterCount() == 1) {
                try {
                    Class<?> parameterClass = m.getParameters()[0].getType();
                    if (String.class.isAssignableFrom(parameterClass)) {
                        m.invoke(this, v);
                    } else if (boolean.class.isAssignableFrom(parameterClass)) {
                        m.invoke(this, Boolean.valueOf(v));
                    } else if (int.class.isAssignableFrom(parameterClass)) {
                        m.invoke(this, Integer.valueOf(v));
                    } else if (double.class.isAssignableFrom(parameterClass)) {
                        m.invoke(this, Double.valueOf(v));
                    } else {
                        LOG.warn("Unrecognized parameter type");
                        continue;
                    }
                    LOG.debug("Set {} from config file", key);
                    return;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    LOG.error(e.getMessage(), e);
                }
            }

        }

        LOG.error("Unrecognized property {}", key);
    }



    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setErrors(String errors) {
        this.errors = errors;
    }

    public String getErrors() {
        return errors;
    }

    public boolean isFollowMerges() {
        return followMerges;
    }

    public void setFollowMerges(boolean followMerges) {
        this.followMerges = followMerges;
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


    private ResteasyWebTarget newWebClient() {
        ResteasyClient client =
            new ResteasyClientBuilder()
                .httpEngine(clientHttpEngine)
                .register(new BasicAuthentication(userName, password))
                .build();
        client
            .register(new AddRequestHeadersFilter());

        return client.target(url);
    }


    /**
     * returns the proxied interface as is actually used on the POMS backend.
     * This is (as long as your client's version corresponds) garantueed to be complete and correct.
     */
    public MediaBackendRestService getBackendRestService() {
        if (proxy == null) {
            LOG.info("Creating proxy for {} {}@{}", MediaBackendRestService.class, userName, url);
            proxy = MediaRestClientAspect.proxy(
                this,
                ErrorAspect.proxyErrors(
                    MediaRestClient.LOG, () -> "media rest", MediaBackendRestService.class,
                    newWebClient().proxy(MediaBackendRestService.class)
                )
            );

        }
        return proxy;
    }

    protected <T extends MediaUpdate> T get(final Class<T> type, final String id) {
        try {
            return (T) getBackendRestService().getMedia(Type.valueOf(type).toString(), id, followMerges);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T extends MediaObject> T getFull(final Class<T> type, final String id) {
        try {
            return (T) getBackendRestService().getFullMediaObject(Type.valueOf(type).toString(), id, followMerges);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Returns the program (as an 'update' object'), with the given id. Or <code>null</code> if not found.
     * @param id This can be an URN, MID, or crid.
     * @return
     */
    public ProgramUpdate getProgram(String id) {
        return get(ProgramUpdate.class, id);
    }

    public SegmentUpdate getSegment(String id) {
        return get(SegmentUpdate.class, id);
    }

    public <T extends MediaUpdate> T get(String id) {
        return (T) get(MediaUpdate.class, id);
    }

    public SortedSet<LocationUpdate> cloneLocations(String id) {

        SortedSet<LocationUpdate> result = new TreeSet<>();
        try {
            XmlCollection<LocationUpdate> i = getBackendRestService().getLocations("media", id, true);
            for (LocationUpdate lu : i) {
                lu.setUrn(null);
                result.add(lu);
            }
        } catch(NullPointerException npe) {
            // dammit
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }


    /** add a location to a Program, Segment or Group */
    protected void addLocation(final Type type, final LocationUpdate location, final String id) {
        getBackendRestService().addLocation(type.toString(), location, id, followMerges, errors);
    }

    public void  addLocationToProgram(LocationUpdate location, String programId) {
        addLocation(Type.PROGRAM, location, programId);
    }

    public void  addLocationToSegment(LocationUpdate location, String segmentId) {
        addLocation(Type.SEGMENT, location, segmentId);
    }

    public void  addLocationToGroup(LocationUpdate location, String groupId) {
        addLocation(Type.GROUP, location, groupId);
    }

    protected String set(final Type type, final MediaUpdate update) {

        try {
            Response response = getBackendRestService().update(type.toString(), update, followMerges, errors, lookupCrids);
            String result = response.readEntity(String.class);
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

    public <T extends MediaObject> T getFull(String id) {
        return (T) getFull(MediaObject.class, id);
    }

    public Iterable<MemberUpdate> getGroupMembers(final String id, final int max, final long offset) {
        try {
            return getBackendRestService().getGroupMembers("media", id, offset, max, "ASC", followMerges);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Iterable<MemberUpdate> getGroupMembers(String id) {
        return getGroupMembers(id, defaultMax, 0);
    }

    public Iterable<MemberUpdate> getGroupEpisodes(final String id, final int max, final long offset) {
        try {
            return getBackendRestService().getGroupEpisodes(id, offset, max, "ASC", followMerges);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Iterable<MemberUpdate> getGroupEpisodes(String id) {
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

    public Iterable<MediaListItem> find(MediaForm form)  {
        try {
            return getBackendRestService().find(form, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setDefaultMax(int max) {
        this.defaultMax = max;
    }

	public boolean isLookupCrid() {
		return lookupCrids;
	}

	public void setLookupCrid(boolean lookupCrids) {
		this.lookupCrids = lookupCrids;
	}

    public double getThrottleRate() {
        return this.throttle.getRate();
    }

    /**
     * Sets the number of requests per seconds
     * @param rate
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
    public String toString() {
        return userName + "@" + url;
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
            LOG.warn(userName + "@" + url + " " + cause + ", retrying after 30 s");
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
