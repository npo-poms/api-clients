package nl.vpro.rs.media;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Callable;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXB;

import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

import nl.vpro.domain.media.Group;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.Program;
import nl.vpro.domain.media.Segment;
import nl.vpro.domain.media.search.MediaForm;
import nl.vpro.domain.media.search.MediaListItem;
import nl.vpro.domain.media.update.*;

/**
 * A client for RESTful calls to a running MediaRestController
 * <p/>
 * A client is stateful, so normally you may need create a new one for every call.
 * E.g. use it like this:
 * <pre>
 * protected Client getClient(Map<String, Object> headers) {
 * Client client = new Client();
 * client.setUrl(mediaRsUrl);
 * client.setUserName(userName);
 * client.setPassword(password);
 * client.setErrors(getMail());
 * client.setHeaders(headers);
 * return client;
 * }
 * private void send(ProgramUpdate update) {
 * Client client = getClient(getHeaders());
 * client.set(update);
 * }
 * </pre>
 *
 * @author Michiel Meeuwissen
 */
public class MediaRestClient {

    private static Logger LOG = LoggerFactory.getLogger(MediaRestClient.class);

    private int defaultMax = 50;

    private final RateLimiter throttle = RateLimiter.create(1.0);
    private final RateLimiter asynchronousThrottle = RateLimiter.create(0.4);

    private MediaRestController proxy;

    static enum Type {
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

    public boolean isWaitForRetry() {
        return waitForRetry;
    }

    public void setWaitForRetry(boolean waitForRetry) {
        this.waitForRetry = waitForRetry;
    }

    private ResteasyWebTarget newWebClient() {

        HttpClientContext context = HttpClientContext.create();
        HttpClient httpClient = new DefaultHttpClient();
        ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient, context);

        ResteasyClient client =
            new ResteasyClientBuilder()
                .httpEngine(engine)
                .register(new BasicAuthentication(userName, password))
                .build();

        return client.target(url);
    }

    protected MediaRestController getProxy() {
        if (proxy == null) {
            proxy = newWebClient().proxy(MediaRestController.class);
        }
        return proxy;
    }

    protected <T extends MediaUpdate> T get(final Class<T> type, final String id) {
        return call(new Callable<T>() {
            @Override
            public T call() throws Exception {
                return (T) getProxy().getMedia(Type.valueOf(type).toString(), id);
            }

            @Override
            public String toString() {
                return "get";
            }
        });
    }

    protected <T extends MediaObject> T getFull(final Class<T> type, final String id) {
        return call(new Callable<T>() {
            @Override
            public T call() throws Exception {
                return (T) getProxy().getFullMediaObject(Type.valueOf(type).toString(), id);
            }

            @Override
            public String toString() {
                return "getFull";
            }
        });
    }

    protected <T> T call(Callable<T> callable) {
        throttle();
        while(true) {
            try {
                return callable.call();
            } catch (NotFoundException nfe) {
                return null;
            } catch (ServiceUnavailableException sue) {
                retryAfterWaitOrException(callable.toString() + ": Service unavailable");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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

    /** add a location to a Program, Segment or Group */
    protected void addLocation(final Type type, final LocationUpdate location, final String id) {
        call(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                getProxy().addLocation(type.toString(), location, id, errors);
                return null;
            }
            @Override
            public String toString() {
                return "addLocation";
            }
        });
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
        return call(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Response response = getProxy().update(type.toString(), update, errors, lookupCrids);
                return response.readEntity(String.class);
            }

            @Override
            public String toString() {
                return "set";
            }
        });
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
        return call(new Callable<Iterable<MemberUpdate>>() {
            @Override
            public Iterable<MemberUpdate> call() throws Exception {
                return getProxy().getGroupMembers("media", id, offset, max, "ASC");
            }

            @Override
            public String toString() {
                return "getMembers";
            }
        });
    }

    public Iterable<MemberUpdate> getGroupMembers(String id) {
        return getGroupMembers(id, defaultMax, 0);
    }

    public Iterable<MemberUpdate> getGroupEpisodes(final String id, final int max, final long offset) {
        return call(new Callable<Iterable<MemberUpdate>>() {
            @Override
            public Iterable<MemberUpdate> call() throws Exception {
                return getProxy().getGroupEpisodes(id, offset, max, "ASC");
            }

            @Override
            public String toString() {
                return "getGroupEpisodes";
            }
        });
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
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        JAXB.marshal(form, bytes);
        return call(new Callable<Iterable<MediaListItem>>() {
            @Override
            public Iterable<MediaListItem> call() throws Exception {
                return getProxy().find(new ByteArrayInputStream(bytes.toByteArray()), false);
            }

            @Override
            public String toString() {
                return "find";
            }
        });
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



    private void retryAfterWaitOrException(String action, RuntimeException e) {
        if (!waitForRetry) throw e;
        retryAfterWaitOrException(action + ":" + e.getMessage());

    }
    private void retryAfterWaitOrException(String cause) {
        if (!waitForRetry) throw new RuntimeException(cause);
        try {
            LOG.warn(userName + "@" + url + " " + cause + ", retrying after 30 s");
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    private void throttle() {
        throttle.acquire();
    }


    private void throttleAsynchronous() {
        asynchronousThrottle.acquire();
    }

}
