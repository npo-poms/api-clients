package nl.vpro.api.client.resteasy;

import java.io.IOException;
import java.time.Instant;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXB;

import org.jboss.resteasy.api.validation.ViolationReport;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.domain.classification.ClassificationService;
import nl.vpro.domain.page.update.LinkUpdate;
import nl.vpro.domain.page.update.PageUpdate;
import nl.vpro.domain.page.update.PageUpdateBuilder;
import nl.vpro.domain.page.update.ParagraphUpdate;
import nl.vpro.rs.pages.update.PageUpdateRestService;

import static org.junit.Assert.assertEquals;


@Ignore("This required running server at publish-dev")
public class PageUpdateApiClientTest {

    private static PageUpdateApiClient clients;



    @BeforeClass
    public static void setUp() throws IOException {
        clients = PageUpdateApiClient.configured().build();
    }

    @Test
    public void testSave() throws Exception {
        PageUpdateRestService client = clients.getPageUpdateRestService();
        PageUpdate instance = PageUpdateBuilder.article("http://www.meeuw.org/test/1234")
            .title("my title " + Instant.now())
            .broadcasters("VPRO").build();
        Response response = client.save(instance);
        if (response.getStatus() == 400) {
            ViolationReport report = response.readEntity(ViolationReport.class);
            JAXB.marshal(report, System.out);
            JAXB.marshal(instance, System.out);

        }
        assertEquals(202, response.getStatus());
    }


    @Test
    public void testSaveTopStory() throws Exception {
        PageUpdateRestService client = clients.getPageUpdateRestService();
        PageUpdate page = PageUpdateBuilder.article("http://www.meeuw.org/test/topstory")
            .title("supergoed, dit! (" + Instant.now() + ")")
            .paragraphs(ParagraphUpdate.of("paragraaf1", "bla bla, blie blie"), ParagraphUpdate.of("alinea 2", "bloe bloe"))
            .broadcasters("VPRO")
            .build();
        JAXB.marshal(page, System.out);

        Response response = client.save(page);
        if (response.getStatus() == 400) {
            ViolationReport report = response.readEntity(ViolationReport.class);
            JAXB.marshal(report, System.out);
            JAXB.marshal(page, System.out);

        }
        assertEquals(202, response.getStatus());
    }


    @Test
    public void testSaveWithTopStory() throws Exception {
        PageUpdateRestService client = clients.getPageUpdateRestService();
        PageUpdate page = PageUpdateBuilder.article("http://www.meeuw.org/test/page_with_topstory")
            .broadcasters("VPRO")
            .lastModified(Instant.now())
            .title("Page with topstory (" + Instant.now() + ")")
            .links(LinkUpdate.topStory("http://www.meeuw.org/test/topstory", "heel goed artikel"))
            .build();
        Response response = client.save(page);
        JAXB.marshal(page, System.out);

        if (response.getStatus() == 400) {
            ViolationReport report = response.readEntity(ViolationReport.class);
            JAXB.marshal(report, System.out);

        }
        assertEquals(202, response.getStatus());

    }


    @Test
    public void testDelete() throws Exception {
        PageUpdateRestService client = clients.getPageUpdateRestService();
        Response response = client.delete("http://www.meeuw.org/test/1234", false, 1);
        if (response.getStatus() == 400) {
            ViolationReport report = response.readEntity(ViolationReport.class);
            JAXB.marshal(report, System.out);

        }
        assertEquals(202, response.getStatus());

    }


    @Test
    public void testDeleteMultiple() throws Exception {
        PageUpdateRestService client = clients.getPageUpdateRestService();
        Response response = client.delete("http://www.meeuw.org/", true, 100);
        if (response.getStatus() == 400) {
            ViolationReport report = response.readEntity(ViolationReport.class);
            JAXB.marshal(report, System.out);

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
