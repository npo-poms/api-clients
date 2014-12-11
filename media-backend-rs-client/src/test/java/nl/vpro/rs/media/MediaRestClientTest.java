package nl.vpro.rs.media;

import javax.xml.bind.JAXB;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.domain.media.MediaType;
import nl.vpro.domain.media.Program;
import nl.vpro.domain.media.search.MediaForm;
import nl.vpro.domain.media.search.MediaListItem;
import nl.vpro.domain.media.search.Pager;
import nl.vpro.domain.media.search.TitleForm;
import nl.vpro.domain.media.support.OwnerType;
import nl.vpro.domain.media.support.TextualType;
import nl.vpro.domain.media.update.GroupUpdate;
import nl.vpro.domain.media.update.ProgramUpdate;

import static org.fest.assertions.Assertions.assertThat;

/**
/**
 * @author Michiel Meeuwissen
 */
@Ignore("needs a running rest-service (TODO: make an integration test)")
public class MediaRestClientTest {

    private MediaRestClient client;

    @Before
    public void setUp() {
        XTrustProvider.install(); // api-dev doesn't have real certificates, and I can't be bothered any more.

        client = new MediaRestClient();
        client.setUserName("vpro-mediatools");
        client.setPassword("Id7shuu7");
        //client.setUrl("http://localhost:8071/rs/");
        //client.setUrl("https://api-dev.poms.omroep.nl/");
        client.setUrl("https://api-test.poms.omroep.nl/");
        client.setThrottleRate(50);
        client.setWaitForRetry(true);

    }

    @Test
    public void client() {
        Program full = client.getFullProgram("12072131");
        System.out.println("update: " + full);
        System.out.println("update: " + full.getBroadcasters());
        ProgramUpdate update = client.getProgram("12072131");
        System.out.println("update: " + update);
        System.out.println("update: " + update.getBroadcasters());
        ProgramUpdate update2 = client.get("WO_VPRO_034420");
        System.out.println("update: " + update2);
    }

    @Test
    public void test() {
        GroupUpdate group = client.getGroup("POMS_S_VPRO_216888");
        System.out.println(group.getBroadcasters());
    }


    @Test
    public void loadCrid() {
        ProgramUpdate update3 = client.get("crid://tmp.fragment.mmbase.vpro.nl/43084334");
        System.out.println("update: " + update3);


    }


    @Test
    public void find()  {
        MediaForm mediaForm = new MediaForm(new Pager(2));
        //mediaForm.setText("dino");
        TitleForm titleForm = new TitleForm("Odd Blood", TextualType.MAIN, OwnerType.BROADCASTER, false);
        mediaForm.addType(MediaType.ALBUM);
        mediaForm.setReleaseYear(null);

        /*// TODO. what if the album has multiple artists?
        RelationForm artistRelation = new RelationForm("ARTIST", "VPRO", null, "Yeasayer");
        mediaForm.addRelation(artistRelation);*/
        mediaForm.addTitle(titleForm);
        Iterable<MediaListItem> result = client.find(mediaForm);
        System.out.println("update: " + result);

    }

	@Test
	public void post() {
		ProgramUpdate update = JAXB.unmarshal(getClass().getResourceAsStream("/POMS_VPRO_216532.xml"), ProgramUpdate.class);
        client.setLookupCrid(false);
		String result = client.set(update);

        assertThat(result).isEqualTo("POMS_VPRO_216532");
	}

    /*@Test
    public void testCouchDBClient() throws MalformedURLException {
        List<Map<String, Object>> media = new ArrayList<>();

        URL url = new URL("http://docs.poms.omroep.nl");
        String hostname = url.getHost();
        int port = url.getPort() == -1 ? 80 : url.getPort();
        final Server server = new ServerImpl(hostname, port);
        Database mediaDatabase  = new Database(server, "poms");
        List<String> urns = Arrays.asList(
            "urn:vpro:media:group:10260683",
            "urn:vpro:media:group:3126881",
            "urn:vpro:media:group:3152589",
            "urn:vpro:media:group:10571721");
        ViewAndDocumentsResult<Map, Map> progs = mediaDatabase.queryDocumentsByKeys(Map.class, Map.class, urns, new Options(), new JSONParser());
        for (ValueAndDocumentRow<Map, Map> row : progs.getRows()) {
            Map<String, Object> document= row.getDocument();
            if (document != null) {
                media.add(document);
            }
        }

        System.out.println("" + media);
    }*/

}
