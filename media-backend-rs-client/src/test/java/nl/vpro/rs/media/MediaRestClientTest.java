package nl.vpro.rs.media;

import java.io.IOException;
import java.time.Instant;

import javax.xml.bind.JAXB;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.domain.image.ImageType;
import nl.vpro.domain.media.*;
import nl.vpro.domain.media.search.MediaForm;
import nl.vpro.domain.media.search.MediaListItem;
import nl.vpro.domain.media.search.Pager;
import nl.vpro.domain.media.search.TitleForm;
import nl.vpro.domain.media.support.OwnerType;
import nl.vpro.domain.media.support.TextualType;
import nl.vpro.domain.media.update.*;

import static org.fest.assertions.Assertions.assertThat;

/**
/**
 * @author Michiel Meeuwissen
 */
@Ignore("needs a running rest-service (TODO: make an integration test)")
public class MediaRestClientTest {

    private MediaRestClient client;

    @Before
    public void setUp() throws IOException {
        client = new MediaRestClient().configured();
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


    @Test(expected = nl.vpro.rs.media.ResponseError.class)
    public void postInvalid() {
        ProgramUpdate update = JAXB.unmarshal(getClass().getResourceAsStream("/POMS_VPRO_216532.xml"), ProgramUpdate.class);
        update.getTitles().clear();
        client.setLookupCrid(false);
        String result = client.set(update);

        assertThat(result).isEqualTo("POMS_VPRO_216532");
    }


    @Test
    public void addMemberOf() {
        String groupCrid = "crid://poms.omroep.nl/testcases/nl.vpro.rs.media.MediaRestClientTest";
        MediaBuilder.GroupBuilder group = MediaBuilder.group(GroupType.COLLECTION)
            .crids(groupCrid)
            .avType(AVType.MIXED)
            .broadcasters("VPRO")
            .mainTitle("Deze group gebruiken we in een junit test");
        GroupUpdate groupUpdate = GroupUpdate.create(group);
        String groupMid = client.set(groupUpdate);
        System.out.println("Found group " + groupMid);

        MediaBuilder.ProgramBuilder program = MediaBuilder.program(ProgramType.CLIP)
            .avType(AVType.AUDIO)
            .broadcasters("VPRO")
            .mainTitle("Test " + Instant.now());
        ProgramUpdate update = ProgramUpdate.create(program);

        update.getMemberOf().add(new MemberRefUpdate(1, groupCrid));
        String mid = client.set(update);
        System.out.println("Created " + mid);


    }

    @Test
    public void removeMemberOf() {
        String groupCrid = "crid://poms.omroep.nl/testcases/nl.vpro.rs.media.MediaRestClientTest";

        GroupUpdate groupUpdate = client.get(groupCrid);
        Iterable<MemberUpdate> members = client.getGroupMembers(groupCrid);

        for (MemberUpdate member : members) {
            MediaUpdate<?> update = member.getMediaUpdate();
            update.getMemberOf().removeIf(m -> m.getMediaRef().equals(groupUpdate.getMid()));
            client.set(update);
            System.out.println("Removed " + update.getMid() + " from group");
            break;
        }
    }

    @Test
    public void copyVisibleLocations() {
        ProgramUpdate program = client.get("POMS_VPRO_158078");

        ProgramUpdate newProgram = new ProgramUpdate();
        newProgram.setType(ProgramType.CLIP);
        newProgram.setAVType(AVType.VIDEO);
        newProgram.setBroadcasters("VPRO");
        newProgram.setMainTitle("bla");


        // TODO. Isn't this odd?
        program.getLocations().forEach(l -> {
            l.setUrn(null);
        });

        newProgram.setLocations(program.getLocations());

        System.out.println(client.set(newProgram));
    }

    @Test
    public void copyLocations() {

        ProgramUpdate newProgram = new ProgramUpdate();
        newProgram.setType(ProgramType.CLIP);
        newProgram.setAVType(AVType.VIDEO);
        newProgram.setBroadcasters("VPRO");
        newProgram.setMainTitle("bla");

        newProgram.setLocations(client.cloneLocations("VPRO_1142324"));

        System.out.println(client.set(newProgram));
    }

    @Test
    public void copyLocations2() throws IOException {

        ProgramUpdate existing = client.get("POMS_VARA_256131");
        existing.setLocations(client.get("POMS_VPRO_1419526").getLocations());

        for (TitleUpdate o : existing.getTitles()) {
            if (o.getType() == TextualType.MAIN) {
                o.setTitle(o.getTitle() + " x");
            }
        }


        JAXB.marshal(existing, System.out);
        System.out.println(client.set(existing));
    }

    @Test
    // Shows MSE-3224
    public void copyLocations3() {
        ProgramUpdate newProgram = new ProgramUpdate();
        newProgram.setType(ProgramType.CLIP);
        newProgram.setAVType(AVType.VIDEO);
        newProgram.setBroadcasters("VPRO");
        newProgram.setMainTitle("bla " + Instant.now());

        newProgram.setLocations(client.get("WO_BNN_351473").getLocations());

        JAXB.marshal(newProgram, System.out);
        System.out.println(client.set(newProgram));

    }


    @Test
    // Shows MSE-3224
    public void copyImages() {
        ProgramUpdate newProgram = new ProgramUpdate();
        newProgram.setType(ProgramType.CLIP);
        newProgram.setAVType(AVType.VIDEO);
        newProgram.setBroadcasters("VPRO");
        newProgram.setMainTitle("bla " + Instant.now());

        newProgram.setImages(client.get("WO_MAX_382054").getImages());

        JAXB.marshal(newProgram, System.out);
        System.out.println(client.set(newProgram));

    }


    @Test
    public void addRelation() {
        RelationDefinition artist = RelationDefinition.of("ARTIST", "VPRO");
        ProgramUpdate program = client.get("POMS_VPRO_1419533");
        // create relation with source program
        program.getRelations().add(RelationUpdate.text(artist, "BLA " + Instant.now()));

        JAXB.marshal(program, System.out);

        System.out.println(client.set(program));


    }


    @Test
    public void addImage() {
        String program = sampleProgram("addImage").id;

        client.getBackendRestService().addImage(
            new ImageUpdate(ImageType.PICTURE, "bla", null, new ImageLocation("http://files.vpro.nl/bril/brillen/bril.png")),
            "media",
            program,
            client.isFollowMerges(),
            null
        );
        System.out.println("Added image to " + program);


    }


    @Test(expected = ResponseError.class)
    public void addImage404() {
        String program = sampleProgram("addImage").id;

        client.getBackendRestService().addImage(
            new ImageUpdate(ImageType.PICTURE, "bla", null, new ImageLocation("http://files.vpro.nl/bril/brillen/BESTAATNIET.png")),
            "media",
            program,
            client.isFollowMerges(),
            null
        );

    }

    @Test
    public void testGetGroup() {
        GroupUpdate group = client.getGroup("POMS_S_VPRO_1416538");
        JAXB.marshal(group, System.out);
    }

    @Test
    public void testCreateWitPortal() {
        WithId<ProgramUpdate> sample = sampleProgram("withPortal");
        ProgramUpdate update = sample.update;
        update.setBroadcasters("EO");
        update.setPortalRestrictions("NETINNL");
        update.setPortals("NETINNL");

        client.set(sample.update);

        System.out.println(sample.id);
    }

    protected WithId<ProgramUpdate> sampleProgram(String test) {
        String crid = "crid://poms.omroep.nl/testcases/nl.vpro.rs.media.MediaRestClientTest/" + test;
        MediaBuilder.ProgramBuilder program = MediaBuilder.program(ProgramType.CLIP)
            .crids(crid)
            .avType(AVType.MIXED)
            .broadcasters("VPRO")
            .mainTitle("Deze clip gebruiken we in een junit test");
        ProgramUpdate programUpdate = ProgramUpdate.create(program);
        String programMid = client.set(programUpdate);
        System.out.println("" + crid + " ->  " + programMid);
        return new WithId<>(programUpdate, programMid);

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
