package nl.vpro.api.client.resteasy;

import java.net.MalformedURLException;

import org.junit.Before;
import org.junit.Test;

import nl.vpro.domain.classification.ClassificationService;

import static org.junit.Assert.assertEquals;

public class PageUpdateApiClientTest {

    private PageUpdateApiClient clients;

    @Before
    public void setUp() throws MalformedURLException {
        clients = new PageUpdateApiClient(
            "http://publish-dev.poms.omroep.nl/",
            "vpro-cms",
            "***REMOVED***");
    }

    @Test
    public void testGetPageUpdateRestService() throws Exception {
        clients.getPageUpdateRestService();
    }

    @Test
    public void testGetClassificationService() throws Exception {
        ClassificationService classificationService = clients.getClassificationService();
        assertEquals("Jeugd", classificationService.getTerm("3.0.1.1").getName());
        classificationService = clients.getClassificationService();
        assertEquals("Film", classificationService.getTerm("3.0.1.2").getName());
    }
}
