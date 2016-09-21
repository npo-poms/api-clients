package nl.vpro.api.client.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.api.client.resteasy.ApiAuthenticationRequestFilter;
import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.api.Change;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.MediaFormBuilder;
import nl.vpro.domain.api.media.MediaResult;
import nl.vpro.domain.api.media.MediaSearchResult;
import nl.vpro.domain.api.profile.Profile;
import nl.vpro.domain.media.DescendantRef;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaType;
import nl.vpro.util.CloseableIterator;

import static org.fest.assertions.Assertions.assertThat;

@Ignore("This is an integration test")
public class NpoApiClientUtilTest {

    private NpoApiMediaUtil util;

    private NpoApiMediaUtil utilShortTimeout;

    private Instant start;


    @Before
    public void setUp() throws IOException {

        util = new NpoApiMediaUtil(NpoApiClients.configured().build(), new NpoApiRateLimiter());
        utilShortTimeout = new NpoApiMediaUtil(NpoApiClients.configured().setConnectionTimeout(1).build(), new NpoApiRateLimiter());
        start = Instant.now();
        System.out.println("Testing " + util);
    }
    @After
    public void after() {
        System.out.println("Test took "  + Duration.between(start, Instant.now()));
    }

    @Test
    public void testLoadMultiple() throws Exception {
        MediaObject[] result = util.load("AVRO_1656037", "AVRO_1656037", "POMS_VPRO_487567");
        assertThat(result[0].getMid()).isEqualTo("AVRO_1656037");
        assertThat(result[1].getMid()).isEqualTo("AVRO_1656037");
        assertThat(result[2].getMid()).isEqualTo("POMS_VPRO_487567");
        result = util.load("AVRO_1656037", "AVRO_1656037", "VPWON_1167222");
        assertThat(result[0].getMid()).isEqualTo("AVRO_1656037");
        assertThat(result[1].getMid()).isEqualTo("AVRO_1656037");
        assertThat(result[2].getMid()).isEqualTo("VPWON_1167222");
    }




    @Test(expected = IOException.class)
    public void testLoadMultipleWithTimeout() throws Exception {
        MediaObject[] result = utilShortTimeout.load("AVRO_1656037", "AVRO_1656037", "POMS_VPRO_487567");
        System.out.println(Arrays.asList(result));
    }


    @Test
    public void testLoad() throws Exception {
        MediaObject result = util.loadOrNull("AVRO_1656037");
        assertThat(result.getMid()).isEqualTo("AVRO_1656037");
    }


    @Test
    public void testLoadNotFound() throws Exception {
        MediaObject result = util.loadOrNull("bestaat niet");
        assertThat(result).isNull();
    }


    @Test(expected = IOException.class)
    public void testLoadWithTimeout() throws Exception {
        MediaObject result = utilShortTimeout.loadOrNull("AVRO_1656037");
        // it doesn't
        System.out.println("Didn't time out!");
        System.out.println(result.getMid());
    }


    @Test
    @Ignore("Takes long!!")
    public void testChanges() throws Exception {
        CloseableIterator<Change> result = util.changes("woord", 14703333L, Order.ASC, Integer.MAX_VALUE);
        long i = 0;
        while (result.hasNext()) {
            Change next = result.next();
            if (i++ % 10 == 0) {
                System.out.println(next);
                //Thread.sleep(10000);
            }
        }


    }

    @Test
    @Ignore("Takes long!")
    public void testIterate() throws IOException {
        Instant start = Instant.now();
        Iterator<MediaObject> result = util.iterate(new MediaForm(), "vpro");
        long i = 0;
        while (result.hasNext()) {
            MediaObject next = result.next();
            if (i++ % 100 == 0) {
                System.out.println(i + " " + next +  " " + next.getLastPublished());
                //Thread.sleep(10000);
            }
        }
        System.out.println("" + i + " " + Duration.between(start, Instant.now()));
        // couchdb 57355 PT4M45.904S
        // es      51013 PT1M5 .879 S
    }


    @Test
    public void testListDescendants() throws Exception {
        MediaResult result = util.listDescendants("RBX_S_NTR_553927", Order.DESC, input -> input.getMediaType() == MediaType.BROADCAST, 123);
        assertThat(result.getSize()).isEqualTo(123);


    }

    @Test
    public void testListRelated() throws Exception {
        MediaSearchResult result = util.getClients().getMediaService().findRelated(MediaFormBuilder.emptyForm(), "VPWON_1174495", "vpro", null, 10);
        System.out.println(result.asList().get(0).getDescendantOf().iterator().next().getMidRef());
    }



    @Test(expected = IOException.class)
    @Ignore("This doesn't test the api, but httpclient")
    // this does indeed timeout
    public void timeout() throws IOException, URISyntaxException {

        ApacheHttpClient4Engine engine = (ApacheHttpClient4Engine) utilShortTimeout.getClients().getClientHttpEngine();
        HttpRequestBase get = getAuthenticatedRequest();

        StopWatch sw = new StopWatch();
        try {
            sw.start();
            HttpResponse response = engine.getHttpClient().execute(get, (org.apache.http.protocol.HttpContext) null);

            System.out.println("Didn't time out!");
            System.out.println(response.getProtocolVersion());
            System.out.println(response.getStatusLine().getStatusCode());
            System.out.println(response.getStatusLine().getReasonPhrase());
            System.out.println(response.getStatusLine().toString());
            IOUtils.copy(response.getEntity().getContent(), System.out);

        } finally {
            sw.stop();
            System.out.println(sw);

        }

    }

    @Test(expected = IOException.class)
    @Ignore("Doesn't test api, but httpclient")
    public void timeoutWithInvoke() throws URISyntaxException, IOException {
        /*ApacheHttpClient4Engine engine = (ApacheHttpClient4Engine) utilShortTimeout.getClients().getClientHttpEngine();
        String host = target + "api/media/AVRO_1656037";
        URI uri = new URI(host);

        HttpResponse response = engine.getHttpClient().execute(getAuthenticatedRequest(), (org.apache.http.protocol.HttpContext) null);

        //engine.getHttpClient().execute(getAuthenticatedRequest(), null);

        ResteasyProviderFactory providerFactory = new ResteasyProviderFactory();
        ClientConfiguration config = new ClientConfiguration(providerFactory);
        ClientRequestHeaders headers = new ClientRequestHeaders(config);
        Client client = ResteasyClientBuilder.newClient(config);
        ClientInvocation invocation = new ClientInvocation((org.jboss.resteasy.client.jaxrs.ResteasyClient) client, uri, headers, config);
        //ClientInvocation invocation = new ClientInvocation(, uri, new ClientRequestHeaders(configuration), null);


        engine.invoke(invocation);
*/
    }


    @Test
    public void testLoadProfile() throws Exception {
        Profile profile = util.getClients().getProfileService().load("human", null);
        System.out.println(profile.getMediaProfile());
    }

    @Test(expected = BadRequestException.class)
    public void badRequest() throws IOException {
        //MediaForm form = Jackson2Mapper.getInstance().readValue("{\"searches\":{\"mediaIds\":[{\"value\":\"VPWON_1181924\",\"match\":\"not\"}],\"types\":[{\"value\":\"BROADCAST\",\"match\":\"should\"},{\"value\":\"CLIP\",\"match\":\"should\"},{\"value\":\"SEGMENT\",\"match\":\"should\"},{\"value\":\"TRACK\",\"match\":\"should\"}]}}", MediaForm.class);
        /*String seriesRef = getSeriesRef(util.getClients().getMediaService().load("VPWON_1229797", null));
        System.out.println("" + seriesRef);*/
        System.out.println(util.getClients().getMediaService().findDescendants(new MediaForm(), "POMS_S_VPRO_522965", "vpro", "title,description,image", 0L, 1000).asList());

    }


    protected String getSeriesRef(MediaObject media) {
        String seriesRef = null;
        for (DescendantRef descendantRef : media.getDescendantOf()) {
            if (descendantRef.getType() == MediaType.SEASON) {
                seriesRef = descendantRef.getMidRef();
                // here also might be a series, which is what we prefer
            } else if (descendantRef.getType() == MediaType.SERIES) {
                seriesRef = descendantRef.getMidRef();
                break; // we found a series it belongs too!
            }
        }
        return seriesRef;
    }

    private HttpRequestBase getAuthenticatedRequest() throws URISyntaxException {
        //String host = "http://fake-response.appspot.com/?sleep=7";
        String host = utilShortTimeout.getClients().getBaseUrl() + "api/media/AVRO_1656037";
        URI uri = new URI(host);
        ApiAuthenticationRequestFilter authentication = utilShortTimeout.getClients().getAuthentication();


        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        authentication.authenticate(uri, headers);
        HttpRequestBase httpget = new HttpGet(host);
        for (Map.Entry<String, List<Object>> e : headers.entrySet()) {
            for (Object o : e.getValue()) {
                httpget.addHeader(e.getKey(), String.valueOf(o));
            }
        }
        return httpget;
    }

}
