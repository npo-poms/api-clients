package nl.vpro.api.client.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.StringReader;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXB;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.api.client.resteasy.PageUpdateApiClient;
import nl.vpro.domain.page.PageType;
import nl.vpro.domain.page.update.PageUpdate;
import nl.vpro.util.Env;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore("This required running server at publish-dev")
@Slf4j
public class PageUpdateApiClientUtilTest {

    private PageUpdateApiUtil util;

    @Before
    public void setUp() {
        PageUpdateApiClient clients = PageUpdateApiClient
            .configured(Env.LOCALHOST)
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .build();
        util = PageUpdateApiUtil.builder().client(clients).build();
    }

    @Test
    public void testSaveInvalid() {
        PageUpdate instance = new PageUpdate(PageType.ARTICLE, "http://vpro.nl/test");
        Result result = util.save(instance);
        assertThat(result.getStatus()).isEqualTo(Result.Status.INVALID);
        assertThat(result.getErrors()).contains("may not be null");
    }

    @Test
    public void testDelete() {
        String id  = "http://BESTAATNIET";
        Result result = util.delete(id);
        assertThat(result.getStatus()).isEqualTo(Result.Status.SUCCESS);
        assertThat(result.getErrors()).isNull();
    }

    @Test
    public void accesDenied() {
        String willCauseDeny = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<pageUpdate:page xmlns:pageUpdate=\"urn:vpro:pages:update:2013\" xmlns:pages=\"urn:vpro:pages:2013\" type=\"PLAYER\" url=\"http://cultura-sample.localhost/speel.RBX_NTR_632457.html\" publishStart=\"2014-09-23T19:00:00+02:00\">\n" +
            "    <pageUpdate:crid>crid://vpro/media/cultura/RBX_NTR_632457</pageUpdate:crid>\n" +
            "    <pageUpdate:broadcaster>NTR</pageUpdate:broadcaster>\n" +
            "    <pageUpdate:title>Blaudzun te gast met zijn favoriete platen</pageUpdate:title>\n" +
            "    <pageUpdate:embeds>\n" +
            "        <pageUpdate:embed midRef=\"RBX_NTR_632457\">\n" +
            "            <pageUpdate:title>Blaudzun te gast met zijn favoriete platen</pageUpdate:title>\n" +
            "            <pageUpdate:description>Een grote bril, een ietwat apart kapsel en een gitaar. Dat is Johannes Sigmond uit Arnhem, ook wel bekend als Blaudzun. Blaudzun was een gegarandeerd feest op elk festival van afgelopen zomer! Vanavond komt deze singer-songwriter naar Winfrieds Woonkamer om zijn favoriete platen aan jou te laten horen. Zijn lijst van dit uur:1. The Jig - Bike Ride2. Prince - I Would Die 4 You3. Child Of Lov ft Damon Albarn - One Day4. Anne Murray - Paths Of Victory5. Outkast - Roses6. Mulatu Astatke - Yekermo Sew7. Earl Sweatshirt - Burgundy 8. Nina Simone - Sinnerman9. TLC - Waterfalls10. The Roots - Singing Man</pageUpdate:description>\n" +
            "        </pageUpdate:embed>\n" +
            "    </pageUpdate:embeds>\n" +
            "    <pageUpdate:image type=\"PICTURE\">\n" +
            "        <pageUpdate:title>Blaudzun_-_2012-05-05.jpg</pageUpdate:title>\n" +
            "        <pageUpdate:description>Blaudzun te gast met zijn favoriete platen</pageUpdate:description>\n" +
            "        <pageUpdate:imageLocation>\n" +
            "            <pageUpdate:url>http://images.poms.omroep.nl/image/s360/289017.jpg</pageUpdate:url>\n" +
            "        </pageUpdate:imageLocation>\n" +
            "    </pageUpdate:image>\n" +
            "</pageUpdate:page>\n";
        //System.out.println(willCauseError);
        PageUpdate update = JAXB.unmarshal(new StringReader(willCauseDeny), PageUpdate.class);
        Result result = util.save(update);
        assertThat(result.getStatus()).isEqualTo(Result.Status.DENIED);
        assertThat(result.getErrors()).contains("Access is denied");
    }

    @Test
    public void deleteWhereStartsWith() {
        log.info("{}", util);
        Result result = util.deleteWhereStartsWith("http://bla/bloe");
        log.info("{}", result.getEntity());
    }
}
