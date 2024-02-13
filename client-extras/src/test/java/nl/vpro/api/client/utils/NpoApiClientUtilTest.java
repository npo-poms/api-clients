package nl.vpro.api.client.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.internal.*;
import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.jupiter.api.*;

import nl.vpro.api.client.frontend.ApiAuthenticationRequestFilter;
import nl.vpro.api.client.frontend.NpoApiClients;
import nl.vpro.domain.api.MediaChange;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.media.*;
import nl.vpro.domain.api.profile.Profile;
import nl.vpro.domain.media.MediaType;
import nl.vpro.domain.media.*;
import nl.vpro.util.CloseableIterator;
import nl.vpro.util.CountedIterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Disabled("This is an integration test")
@Slf4j
public class NpoApiClientUtilTest {

    private NpoApiMediaUtil util;

    private NpoApiMediaUtil utilShortTimeout;

    private Instant start;


    @BeforeEach
    public void setUp() {

        util = new NpoApiMediaUtil(NpoApiClients.configured().warnThreshold(Duration.ofMillis(1)).build(), new NpoApiRateLimiter());

        utilShortTimeout = new NpoApiMediaUtil(
            NpoApiClients.configured()
                .connectTimeout(Duration.ofMillis(1)).build(), new NpoApiRateLimiter());
        start = Instant.now();
        log.info("Testing {}", util);
    }
    @AfterEach
    public void after() {
        log.info("Test took {}", Duration.between(start, Instant.now()));
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




    @Test
    public void testLoadMultipleWithTimeout() {
        assertThatThrownBy(() -> {
            MediaObject[] result = utilShortTimeout.load("AVRO_1656037", "AVRO_1656037", "POMS_VPRO_487567");
            log.info("{}", Arrays.asList(result));
        }).isInstanceOf(IOException.class);
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


    @Test
    public void testLoadWithTimeout() {
        assertThatThrownBy(() -> {

            MediaObject result = utilShortTimeout.loadOrNull("AVRO_1656037");
            // it doesn't
            log.info("Didn't time out!");
            log.info(result.getMid());
        }).isInstanceOf(IOException.class);
    }


    @SuppressWarnings("deprecation")
    @Test
    @Disabled("Takes long!!")
    public void testChanges() throws Exception {
        try (CloseableIterator<MediaChange> result = util.changes("woord", 1433329965809L, Order.ASC, Integer.MAX_VALUE)) {
            long i = 0;
            while (result.hasNext()) {
                MediaChange next = result.next();
                if (i++ % 10 == 0) {
                    log.info("{}", next);
                    //Thread.sleep(10000);
                }
            }
        }


    }


    @Test
    @Disabled("Takes long!!")
    public void testChangesEpoch() throws Exception {
        try (CountedIterator<MediaChange> result = util.changes("woord", Instant.EPOCH, Order.ASC, null)) {
            long i = 0;
            while (result.hasNext()) {
                MediaChange next = result.next();
                if (i++ % 1000 == 0) {
                    log.info("{} {}", result.getCount(), next);
                    //Thread.sleep(10000);
                }
            }
        }
    }



    @Test
    @Disabled("Takes long!!")
    public void testChangesVpronl() throws Exception {
        try (CountedIterator<MediaChange> result = util.changes("vpro-predictions", Instant.now().minus(Duration.ofHours(6)), Order.ASC, null)) {
            long i = 0;
            while (result.hasNext()) {
                MediaChange next = result.next();
                log.info("{} {}", result.getCount(), next);
            }
        }
    }



    @Test
    @Disabled("Takes long!")
    public void testIterate() throws Exception {
        Instant start = Instant.now();
        try (CloseableIterator<MediaObject> result = util.iterate(new MediaForm(), null)) {
            long i = 0;
            while (result.hasNext()) {
                MediaObject next = result.next();
                if (i++ % 100 == 0) {
                    log.info(i + " " + next + " " + next.getLastPublishedInstant());
                    //Thread.sleep(10000);
                }
                if (i > 500) {
                    log.info("Breaking");
                    break;
                }
            }
            System.out.println(i + " " + Duration.between(start, Instant.now()));
            // couchdb 57355 PT4M45.904S
            // es      51013 PT1M5 .879 S
        }
    }


    @Test
    public void testListDescendants() {
        MediaResult result = util.listDescendants("RBX_S_NTR_553927", Order.DESC, input -> input.getMediaType() == MediaType.BROADCAST, 123);
        assertThat(result.getSize()).isEqualTo(123);


    }

    @Test
    public void testListRelated() {
        MediaSearchResult result = util.getClients().getMediaService().findRelated(MediaFormBuilder.emptyForm(), "VPWON_1174495", "vpro", null, 10, null);
        log.info("{}", result.asList().get(0).getDescendantOf().iterator().next().getMidRef());
    }



    @Test
    //@Test
    @Disabled("This doesn't test the api, but httpclient")
    // this does indeed timeout
    public void timeout() {
        assertThatThrownBy(() -> {
            HttpClient client;
            {
                ApacheHttpClient43Engine  engine = (ApacheHttpClient43Engine ) utilShortTimeout.getClients().getClientHttpEngine();
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
        }).isInstanceOf(IOException.class);

    }

    //@Test(expected = IOException.class)
    @Test
    @Disabled("Doesn't test api, but httpclient")
    public void timeoutWithInvoke() throws URISyntaxException {
        ApacheHttpClient43Engine  engine = (ApacheHttpClient43Engine ) utilShortTimeout.getClients().getClientHttpEngine();
        String host = utilShortTimeout.getClients().getBaseUrl() + "/media/AVRO_1656037";
        URI uri = new URI(host);
        System.out.println("Testing " + uri);
        //HttpGet get = getAuthenticatedRequest(uri);
        //HttpResponse response = engine.getHttpClient().execute(get, (org.apache.http.protocol.HttpContext) null);

        //engine.getHttpClient().execute(getAuthenticatedRequest(), null);

        ResteasyProviderFactory providerFactory = new ResteasyProviderFactoryImpl();
        ClientConfiguration config = new ClientConfiguration(providerFactory);
        ClientRequestHeaders headers = new ClientRequestHeaders(config);
        Client client = ResteasyClientBuilder.newClient(config);
        ClientInvocation invocation = new ClientInvocation((org.jboss.resteasy.client.jaxrs.ResteasyClient) client, uri, headers, config);
        invocation.setMethod("GET");
        //ClientInvocation invocation = new ClientInvocation(, uri, new ClientRequestHeaders(configuration), null);


        try (Response response = engine.invoke(invocation)) {
            log.debug("{}", response);
        }
    }


    @Test
    public void testLoadProfile() {
        Profile profile = util.getClients().getProfileService().load("human");
        System.out.println(profile.getMediaProfile());
    }

    @Test
    public void badRequest() {
        assertThatThrownBy(() -> {

        //MediaForm form = Jackson2Mapper.getInstance().readValue("{\"searches\":{\"mediaIds\":[{\"value\":\"VPWON_1181924\",\"match\":\"not\"}],\"types\":[{\"value\":\"BROADCAST\",\"match\":\"should\"},{\"value\":\"CLIP\",\"match\":\"should\"},{\"value\":\"SEGMENT\",\"match\":\"should\"},{\"value\":\"TRACK\",\"match\":\"should\"}]}}", MediaForm.class);
        /*String seriesRef = getSeriesRef(util.getClients().getMediaService().load("VPWON_1229797", null));
        System.out.println("" + seriesRef);*/
            System.out.println(util.getClients().getMediaService().findDescendants(new MediaForm(), "POMS_S_VPRO_522965", "vpro", "title,description,image", 0L, 1000).asList());
        }).isInstanceOf(jakarta.ws.rs.NotFoundException.class);
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

    private HttpGet getAuthenticatedRequest(URI uri) {
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
