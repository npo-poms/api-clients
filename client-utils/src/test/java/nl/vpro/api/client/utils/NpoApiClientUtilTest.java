package nl.vpro.api.client.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ClientRequestHeaders;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.api.client.resteasy.NpoApiAuthentication;
import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.api.Change;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.util.CloseableIterator;

import static org.fest.assertions.Assertions.assertThat;

//@Ignore("This is an integration test")
public class NpoApiClientUtilTest {

    private NpoApiMediaUtil util;

    private NpoApiMediaUtil utilShortTimeout;


    private String target = "http://rs.poms.omroep.nl/v1/";
    ///private String target = "http://rs-test.poms.omroep.nl/v1/";
    //private String target = "http://localhost:8070/v1/";

    @Before
    public void setUp() throws MalformedURLException {
        {
            NpoApiClients clients = new NpoApiClients(
                target,
                "ione7ahfij",
                "***REMOVED***",
                "http://www.vpro.nl", 1000);
            util = new NpoApiMediaUtil(clients, new NpoApiRateLimiter());
        }

        {
            NpoApiClients clients = new NpoApiClients(
                target,
                "ione7ahfij",
                "***REMOVED***",
                "http://www.vpro.nl", 1);
            utilShortTimeout = new NpoApiMediaUtil(clients, new NpoApiRateLimiter());
        }

    }

    @Test
    public void testLoadMultiple() throws Exception {
        MediaObject[] result = util.load("AVRO_1656037", "AVRO_1656037", "POMS_VPRO_487567");
        assertThat(result[0].getMid()).isEqualTo("AVRO_1656037");
        assertThat(result[1].getMid()).isEqualTo("AVRO_1656037");
        assertThat(result[2].getMid()).isEqualTo("POMS_VPRO_487567");
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


    @Test(expected = IOException.class)
    public void testLoadWithTimeout() throws Exception {
        MediaObject result = utilShortTimeout.loadOrNull("AVRO_1656037");
        // it doesn't
        System.out.println("Didn't time out!");
        System.out.println(result.getMid());
    }


    @Test
    public void testChanges() throws Exception {
        CloseableIterator<Change> result = util.changes("woord", 14703333l, Order.ASC, Integer.MAX_VALUE);
        long i = 0;
        while (result.hasNext()) {
            Change next = result.next();
            if (i++ % 10 == 0) {
                System.out.println(next);
                //Thread.sleep(10000);
            }
        }


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
        ApacheHttpClient4Engine engine = (ApacheHttpClient4Engine) utilShortTimeout.getClients().getClientHttpEngine();
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

    }

    private HttpRequestBase getAuthenticatedRequest() throws URISyntaxException {
        //String host = "http://fake-response.appspot.com/?sleep=7";
        String host = target + "api/media/AVRO_1656037";
        URI uri = new URI(host);
        NpoApiAuthentication authentication = utilShortTimeout.getClients().getAuthentication();


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
