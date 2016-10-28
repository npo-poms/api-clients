package nl.vpro.api.client.utils;

import java.net.MalformedURLException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.page.Page;

@Ignore("This is an integration test")
public class NpoApiPageUtilTest {

    private NpoApiPageUtil util;


    private String target = "http://rs.poms.omroep.nl/v1/";
    //private String target = "http://rs-dev.poms.omroep.nl/v1/";
    //private String target = "http://rs-test.poms.omroep.nl/v1/";
    //private String target = "http://localhost:8070/v1/";

    @Before
    public void setUp() throws MalformedURLException {
        {
            NpoApiClients clients = new NpoApiClients(
                target,
                "ione7ahfij",
                "***REMOVED***",
                "http://www.vpro.nl",
                10000,
                true
            );
            util = new NpoApiPageUtil(clients, new NpoApiRateLimiter());
        }

    }

    @Test
    public void testLoadMultiple() throws Exception {
        Page[] result = util.loadByMid(Arrays.asList("vpro", null), null, "AVRO_1656037", "AVRO_1656037", "POMS_VPRO_487567", "BLOE_234");
		System.out.println(Arrays.asList(result));

    }


}
