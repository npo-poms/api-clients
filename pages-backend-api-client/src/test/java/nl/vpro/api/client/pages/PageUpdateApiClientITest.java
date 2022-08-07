package nl.vpro.api.client.pages;

import java.time.Instant;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXB;

import org.jboss.resteasy.api.validation.ViolationReport;
import org.junit.jupiter.api.*;

import nl.vpro.domain.classification.ClassificationService;
import nl.vpro.domain.page.PageIdMatch;
import nl.vpro.domain.page.update.*;
import nl.vpro.rs.pages.update.PageUpdateRestService;
import nl.vpro.util.Env;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


@Disabled("This required running server at publish-dev")
public class PageUpdateApiClientITest {

    private static PageUpdateApiClient clients;



    @BeforeAll
    public static void setUp() {
        clients = PageUpdateApiClient.configured(Env.DEV).build();
    }

    @Test
    public void testSave() {
        PageUpdateRestService client = clients.getPageUpdateRestService();
        PageUpdateRestService client2 = clients.getPageUpdateRestService();

        assertThat(client).isSameAs(client2);
        PageUpdate instance = PageUpdateBuilder.article("http://www.meeuw.org/test/1234")
            .title("my title " + Instant.now())
            .broadcasters("VPRO").build();
        try (Response response = client.save(instance, null)) {
            if (response.getStatus() == 400) {
                ViolationReport report = response.readEntity(ViolationReport.class);
                JAXB.marshal(report, System.out);
                JAXB.marshal(instance, System.out);

            }
            assertEquals(202, response.getStatus());
        }
    }


    @Test
    public void testSaveTopStory() {
        PageUpdateRestService client = clients.getPageUpdateRestService();
        PageUpdate page = PageUpdateBuilder.article("http://www.meeuw.org/test/topstory")
            .title("supergoed, dit! (" + Instant.now() + ")")
            .paragraphs(ParagraphUpdate.of("paragraaf1", "bla bla, blie blie"), ParagraphUpdate.of("alinea 2", "bloe bloe"))
            .broadcasters("VPRO")
            .build();
        JAXB.marshal(page, System.out);

        try (Response response = client.save(page, null)) {
            if (response.getStatus() == 400) {
                ViolationReport report = response.readEntity(ViolationReport.class);
                JAXB.marshal(report, System.out);
                JAXB.marshal(page, System.out);

            }
            assertEquals(202, response.getStatus());
        }
    }


    @Test
    public void testSaveWithTopStory() {
        PageUpdateRestService client = clients.getPageUpdateRestService();
        PageUpdate page = PageUpdateBuilder.article("http://www.meeuw.org/test/page_with_topstory")
            .broadcasters("VPRO")
            .lastModified(Instant.now())
            .title("Page with topstory (" + Instant.now() + ")")
            .links(LinkUpdate.topStory("http://www.meeuw.org/test/topstory", "heel goed artikel"))
            .build();
        try (Response response = client.save(page, null)) {
            JAXB.marshal(page, System.out);

            if (response.getStatus() == 400) {
                ViolationReport report = response.readEntity(ViolationReport.class);
                JAXB.marshal(report, System.out);

            }
            assertEquals(202, response.getStatus());
        }

    }


    @Test
    public void testDelete() {
        PageUpdateRestService client = clients.getPageUpdateRestService();
        try (Response response = client.delete("http://www.meeuw.org/test/1234", false, 1, true, PageIdMatch.URL, null)) {

            if (response.getStatus() == 400) {
                ViolationReport report = response.readEntity(ViolationReport.class);
                JAXB.marshal(report, System.out);

            }
            assertEquals(202, response.getStatus());
        }

    }


    @Test
    public void testDeleteMultiple() {
        PageUpdateRestService client = clients.getPageUpdateRestService();
        try (Response response = client.delete("http://www.meeuw.org/", true, 100, true, PageIdMatch.URL, null)) {
            if (response.getStatus() == 400) {
                ViolationReport report = response.readEntity(ViolationReport.class);
                JAXB.marshal(report, System.out);

            }
            assertEquals(202, response.getStatus());
        }

    }

    @Test
    public void testGetClassificationService() {
        ClassificationService classificationService = clients.getClassificationService();
        assertEquals("Jeugd", classificationService.getTerm("3.0.1.1").getName());
        classificationService = clients.getClassificationService();
        assertEquals("Film", classificationService.getTerm("3.0.1.2").getName());
    }
}
