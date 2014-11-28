package nl.vpro.api.client.utils;

import java.io.StringReader;
import java.net.MalformedURLException;

import javax.xml.bind.JAXB;

import org.junit.Before;
import org.junit.Test;

import nl.vpro.api.client.resteasy.PageUpdateApiClient;
import nl.vpro.domain.page.PageType;
import nl.vpro.domain.page.update.PageUpdate;

import static org.fest.assertions.Assertions.assertThat;

//@Ignore("This required running server at publish-dev")
public class PageUpdateApiClientUtilTest  {

    private PageUpdateApiUtil util;

    private String target = "http://publish-test.pages.omroep.nl/";
    //private String target = "http://localhost:8060/";

    @Before
    public void setUp() throws MalformedURLException {
        PageUpdateApiClient clients = new PageUpdateApiClient(
            target,
            "vpro-cms",
            "***REMOVED***",
            10000);
        util = new PageUpdateApiUtil(clients, new PageUpdateRateLimiter());
    }

    @Test
    public void testSave() throws Exception {
        PageUpdate instance = new PageUpdate(PageType.ARTICLE, "http://vpro.nl/test");
        Result result = util.save(instance);
        assertThat(result.getStatus()).isEqualTo(Result.Status.INVALID);
        System.out.println(result.getErrors());
    }


    @Test
    public void testDelete() throws Exception {
        String id  = "http://BESTAATNIET";
        Result result = util.delete(id);
        assertThat(result.getStatus()).isEqualTo(Result.Status.SUCCESS);

        System.out.println("errors " + result.getErrors());
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
        System.out.println(result.getErrors());
        assertThat(result.getStatus()).isEqualTo(Result.Status.DENIED);


    }
}
