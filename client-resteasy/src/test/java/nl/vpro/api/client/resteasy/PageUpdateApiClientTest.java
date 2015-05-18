package nl.vpro.api.client.resteasy;

import java.net.MalformedURLException;
import java.util.Arrays;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXB;

import org.jboss.resteasy.api.validation.ViolationReport;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.domain.classification.ClassificationService;
import nl.vpro.domain.page.PageType;
import nl.vpro.domain.page.update.PageUpdate;
import nl.vpro.rs.pages.update.PageUpdateRestService;

import static org.junit.Assert.assertEquals;


@Ignore("This required running server at publish-dev")
public class PageUpdateApiClientTest {

    private PageUpdateApiClient clients;



    @Before
    public void setUp() throws MalformedURLException {
        clients = new PageUpdateApiClient(
            "http://publish-test.pages.omroep.nl/",
            "vpro-cms",
            "***REMOVED***", 10000);
    }

    @Test
    public void testGetPageUpdateRestService() throws Exception {
        PageUpdateRestService client = clients.getPageUpdateRestService();
        PageUpdate instance = new PageUpdate(PageType.ARTICLE, "http://vpro.nl/test");
        instance.setTitle("my title");
        instance.setBroadcasters(Arrays.asList("VPRO"));
        Response response = client.save(instance);
        if (response.getStatus() == 400) {
            ViolationReport report = response.readEntity(ViolationReport.class);
            JAXB.marshal(report, System.out);
            JAXB.marshal(instance, System.out);

        }
        assertEquals(202, response.getStatus());

    }

    @Test
    public void testGetClassificationService() throws Exception {
        ClassificationService classificationService = clients.getClassificationService();
        assertEquals("Jeugd", classificationService.getTerm("3.0.1.1").getName());
        classificationService = clients.getClassificationService();
        assertEquals("Film", classificationService.getTerm("3.0.1.2").getName());
    }
}
