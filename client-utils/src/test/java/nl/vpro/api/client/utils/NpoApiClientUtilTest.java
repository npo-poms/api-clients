package nl.vpro.api.client.utils;

import java.net.MalformedURLException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.media.MediaObject;

import static org.fest.assertions.Assertions.assertThat;

@Ignore("This is an integration test")
public class NpoApiClientUtilTest {

    private NpoApiMediaUtil util;

    //private String target = "http://rs-dev.poms.omroep.nl/v1/";
    private String target = "http://localhost:8070/v1/";

    @Before
    public void setUp() throws MalformedURLException {
        NpoApiClients clients = new NpoApiClients(
            target,
            "ione7ahfij",
            "***REMOVED***",
            "http://www.vpro.nl");
        util = new NpoApiMediaUtil(clients);
    }


    @Test
    public void testLoad() throws Exception {
        MediaObject[] result = util.load("AVRO_1656037", "AVRO_1656037", "POMS_VPRO_487567");
        assertThat(result[0].getMid()).isEqualTo("AVRO_1656037");
        assertThat(result[1].getMid()).isEqualTo("AVRO_1656037");
        assertThat(result[2].getMid()).isEqualTo("POMS_VPRO_487567");

    }
}
