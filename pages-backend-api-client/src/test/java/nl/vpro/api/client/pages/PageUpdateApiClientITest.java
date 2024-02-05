package nl.vpro.api.client.pages;

import lombok.extern.log4j.Log4j2;

import java.time.Instant;

import javax.xml.bind.JAXB;

import org.junit.jupiter.api.*;

import nl.vpro.domain.classification.ClassificationService;
import nl.vpro.domain.page.PageIdMatch;
import nl.vpro.domain.page.update.*;
import nl.vpro.rs.pages.update.PageUpdateRestService;
import nl.vpro.util.Env;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


@Disabled("This required running server at publish-test")
@Log4j2
public class PageUpdateApiClientITest {

    private static PageUpdateApiClient clients;


    @BeforeAll
    public static void setUp() {
        clients = PageUpdateApiClient.configured(Env.TEST).build();
    }

    @Test
    public void testSave() {
        PageUpdateRestService client = clients.getPageUpdateRestService();
        PageUpdateRestService client2 = clients.getPageUpdateRestService();

        assertThat(client).isSameAs(client2);
        PageUpdate instance = PageUpdateBuilder.article("http://www.meeuw.org/test/1234")
            .title("my title " + Instant.now())
            .broadcasters("VPRO").build();
        SaveResult saveResult = client.save(instance, null);
        log.info("{}", saveResult);
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

        SaveResult result  = client.save(page, null);
        log.info("{}", result);
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
        SaveResult result = client.save(page, null);
        log.info("{}", result);
    }


    @Test
    public void testDelete() {
        final PageUpdateRestService client = clients.getPageUpdateRestService();
        final DeleteResult deleteResult = client.delete("http://www.meeuw.org/test/1234", false, 1, true, PageIdMatch.URL);
        log.info("{}", deleteResult);

    }


    @Test
    public void testDeleteMultiple() {
        PageUpdateRestService client = clients.getPageUpdateRestService();
        DeleteResult result =  client.delete("http://www.meeuw.org/", true, 100, true, PageIdMatch.URL);
        log.info("{}", result);
    }

    @Test
    public void testGetClassificationService() {
        ClassificationService classificationService = clients.getClassificationService();
        assertEquals("Jeugd", classificationService.getTerm("3.0.1.1").getName());
        classificationService = clients.getClassificationService();
        assertEquals("Film", classificationService.getTerm("3.0.1.2").getName());
    }
}
