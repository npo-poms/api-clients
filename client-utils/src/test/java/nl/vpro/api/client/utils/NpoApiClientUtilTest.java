package nl.vpro.api.client.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ClientRequestHeaders;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.api.client.resteasy.ApiAuthenticationRequestFilter;
import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.api.MediaChange;
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

        util = new NpoApiMediaUtil(NpoApiClients.configured().warnThreshold(Duration.ofMillis(1)).build(), new NpoApiRateLimiter());

        utilShortTimeout = new NpoApiMediaUtil(
            NpoApiClients.configured()
                .connectTimeout(Duration.ofMillis(1)).build(), new NpoApiRateLimiter());
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
        CloseableIterator<MediaChange> result = util.changes("woord", 1433329965809L, Order.ASC, Integer.MAX_VALUE);
        long i = 0;
        while (result.hasNext()) {
            MediaChange next = result.next();
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
    //@Test
    @Ignore("This doesn't test the api, but httpclient")
    // this does indeed timeout
    public void timeout() throws IOException, URISyntaxException {
        HttpClient client;
        {
            ApacheHttpClient4Engine engine = (ApacheHttpClient4Engine) utilShortTimeout.getClients().getClientHttpEngine();
            client = engine.getHttpClient();
        }

        String host = utilShortTimeout.getClients().getBaseUrl() + "/media/AVRO_1656037";
        URI uri = new URI(host);
        HttpGet get = getAuthenticatedRequest(uri);

        StopWatch sw = new StopWatch();
        try {
            sw.start();
            HttpResponse response = client.execute(get, (org.apache.http.protocol.HttpContext) null);

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

    //@Test(expected = IOException.class)
    @Test
    @Ignore("Doesn't test api, but httpclient")
    public void timeoutWithInvoke() throws URISyntaxException, IOException {
        ApacheHttpClient4Engine engine = (ApacheHttpClient4Engine) utilShortTimeout.getClients().getClientHttpEngine();
        String host = utilShortTimeout.getClients().getBaseUrl() + "/media/AVRO_1656037";
        URI uri = new URI(host);
        System.out.println("Testing " + uri);
        //HttpGet get = getAuthenticatedRequest(uri);
        //HttpResponse response = engine.getHttpClient().execute(get, (org.apache.http.protocol.HttpContext) null);

        //engine.getHttpClient().execute(getAuthenticatedRequest(), null);

        ResteasyProviderFactory providerFactory = new ResteasyProviderFactory();
        ClientConfiguration config = new ClientConfiguration(providerFactory);
        ClientRequestHeaders headers = new ClientRequestHeaders(config);
        Client client = ResteasyClientBuilder.newClient(config);
        ClientInvocation invocation = new ClientInvocation((org.jboss.resteasy.client.jaxrs.ResteasyClient) client, uri, headers, config);
        invocation.setMethod("GET");
        //ClientInvocation invocation = new ClientInvocation(, uri, new ClientRequestHeaders(configuration), null);


        engine.invoke(invocation);
    }


    @Test
    public void testLoadProfile() throws Exception {
        Profile profile = util.getClients().getProfileService().load("human", null);
        System.out.println(profile.getMediaProfile());
    }

    @Test(expected = javax.ws.rs.NotFoundException.class)
    public void badRequest() throws IOException {
        //MediaForm form = Jackson2Mapper.getInstance().readValue("{\"searches\":{\"mediaIds\":[{\"value\":\"VPWON_1181924\",\"match\":\"not\"}],\"types\":[{\"value\":\"BROADCAST\",\"match\":\"should\"},{\"value\":\"CLIP\",\"match\":\"should\"},{\"value\":\"SEGMENT\",\"match\":\"should\"},{\"value\":\"TRACK\",\"match\":\"should\"}]}}", MediaForm.class);
        /*String seriesRef = getSeriesRef(util.getClients().getMediaService().load("VPWON_1229797", null));
        System.out.println("" + seriesRef);*/
        System.out.println(util.getClients().getMediaService().findDescendants(new MediaForm(), "POMS_S_VPRO_522965", "vpro", "title,description,image", 0L, 1000).asList());

    }

    @Test
    public void loadSubtiles() {

        System.out.println(util.getClients().getSubtitlesRestService().get("WO_VPRO_025700", Locale.JAPAN));
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

    private HttpGet getAuthenticatedRequest(URI uri) throws URISyntaxException {
        ApiAuthenticationRequestFilter authentication = utilShortTimeout.getClients().getAuthentication();
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        authentication.authenticate(uri, headers);
        HttpGet httpget = new HttpGet(uri);
        for (Map.Entry<String, List<Object>> e : headers.entrySet()) {
            for (Object o : e.getValue()) {
                httpget.addHeader(e.getKey(), String.valueOf(o));
            }
        }
        return httpget;
    }

}
