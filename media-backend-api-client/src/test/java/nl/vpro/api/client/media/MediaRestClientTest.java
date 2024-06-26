package nl.vpro.api.client.media;

import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXB;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.*;

import org.junit.jupiter.api.*;

import nl.vpro.domain.image.ImageType;
import nl.vpro.domain.media.*;
import nl.vpro.domain.media.search.*;
import nl.vpro.domain.media.support.OwnerType;
import nl.vpro.domain.media.support.TextualType;
import nl.vpro.domain.media.update.*;
import nl.vpro.logging.LoggerOutputStream;
import nl.vpro.util.Env;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
/**
 * @author Michiel Meeuwissen
 */
@Disabled("needs a running rest-service (TODO: make an integration test)")
@Slf4j
public class MediaRestClientTest {

    private MediaRestClient client;

    @BeforeEach
    public void setUp() {
        client = MediaRestClient.configured(Env.LOCALHOST).build();
        client.setWaitForRetry(true);

    }
    @AfterEach
    public void shutdown() {
        client.shutdown();
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
    public void loadByCrid() {
        ProgramUpdate update3 = client.get("crid://tmp.fragment.mmbase.vpro.nl/43084334");
        assertThat(update3).isNotNull();
        log.info("update: {}", update3);


    }


    @Test
    public void loadFullByCrid() {
        Program update3 = client.getFull(Program.class, "crid://tmp.fragment.mmbase.vpro.nl/43084334");
        assertThat(update3).isNotNull();
        log.info("update: {}", update3);


    }


    @Test
    public void find()  {
        MediaForm mediaForm = new MediaForm(new MediaPager(2));
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
        ProgramUpdate update = JAXB.unmarshal(getClass().getResourceAsStream("/POMS_VPRO_216532.xml"),
                ProgramUpdate.class);
        client.setLookupCrids(false);
        String result = client.set(update);

        assertThat(result).isEqualTo("POMS_VPRO_216532");
    }


    @Test
    public void postInvalid() {
        assertThatThrownBy(() -> {
            ProgramUpdate update = JAXB.unmarshal(getClass().getResourceAsStream("/POMS_VPRO_216532.xml"), ProgramUpdate.class);
            update.getTitles().clear();
            client.setLookupCrids(false);
            String result = client.set(update);

            assertThat(result).isEqualTo("POMS_VPRO_216532");
        }).isInstanceOf(ResponseError.class);
    }


    @Test
    public void addMemberOf() {
        WithId<GroupUpdate> group = sampleGroup("addMemberOf", "VPRO");

        ProgramUpdate update = sampleProgram("addMemberOf").update;
        update.getMemberOf().add(new MemberRefUpdate(1, group.id));
        String mid = client.set(update);
        System.out.println("Created " + mid);
    }

    @Test
    public void addMemberOfOtherBroadcaster() {
        WithId<GroupUpdate> group = sampleGroup("addMemberOfDisallowed", "VPRO");

        ProgramUpdate update = client.get("EO_101205912");
        update.getMemberOf().add(new MemberRefUpdate(1, group.id));
        String mid = client.set(update);
        System.out.println("Created " + mid);
    }

    @Test
    public void addMemberOfOtherBroadcasterWorkingVersion() throws IOException {
        WithId<GroupUpdate> group = sampleGroup("addMemberOfDisallowed", "VPRO");
        System.out.println(group.id);
        ProgramUpdate update = client.get("EO_101205912");
        update.getMemberOf().add(new MemberRefUpdate(1, group.id));
        try (Response response = client.getBackendRestService().addMemberOf(
            new MemberRefUpdate(1, "POMS_S_VPRO_1421920"/*owner*/), null,  update.getMid()/*member*/, true, null, null)) {
            log.info("Created {}", response.getEntity());
        }
    }


    @Test
    public void addMemberOfOtherBroadcasterBetterVersion() {
        WithId<GroupUpdate> group = sampleGroup("createMember", "VPRO");
        System.out.println(group.id);

        client.createMember(group.id, "EO_101205912", 1);

    }


    @Test
    public void removeMemberOfOtherBroadcasterBetterVersion() {
        WithId<GroupUpdate> group = sampleGroup("createMember", "VPRO");
        System.out.println(group.id);

        client.removeMember(group.id, "EO_101205912", null);

    }

    @Test
    public void removeMemberOf() throws IOException {
        String groupCrid = "crid://poms.omroep.nl/testcases/nl.vpro.rs.media.MediaRestClientTest";

        GroupUpdate groupUpdate = client.get(groupCrid);
        Iterable<MemberUpdate> members = client.getGroupMembers(groupCrid);



        for (MemberUpdate member : members) {
            MediaUpdate<?> update = member.getMediaUpdate();
            for (MemberRefUpdate r : update.getMemberOf()) {
                if (r.getMediaRef().equals(groupUpdate.getMid())) {

                    JAXB.marshal(r, LoggerOutputStream.info(log));
                    //r.setMediaRef(null);
                    log.info("" + groupUpdate + "->" + r.getMediaRef());
                    try (Response response = client.getBackendRestService().removeMemberOf(null, update.getMid(), groupUpdate.getMid(), null, true, null)) {
                        log.info("{}", response);
                    }

                }
            }
            //update.getMemberOf().removeIf(m -> m.getMediaRef().equals(groupUpdate.getMid()));

            log.info("Removed " + update.getMid() + " from group " + groupUpdate.getMid());
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

        log.info(client.set(newProgram));
    }

    @Test
    public void copyLocations() {

        ProgramUpdate newProgram = new ProgramUpdate();
        newProgram.setType(ProgramType.CLIP);
        newProgram.setAVType(AVType.VIDEO);
        newProgram.setBroadcasters("VPRO");
        newProgram.setMainTitle("bla");

        newProgram.setLocations(client.cloneLocations("VPRO_1142324"));

        log.info(client.set(newProgram));
    }

    @Test
    public void copyLocations2() {

        ProgramUpdate existing = client.get("POMS_VARA_256131");

        existing.setLocations(client.get("POMS_VPRO_1419526").getLocations());

        for (TitleUpdate o : existing.getTitles()) {
            if (o.getType() == TextualType.MAIN) {
                o.set(o.get() + " x");
            }
        }


        JAXB.marshal(existing, LoggerOutputStream.info(log));
        log.info(client.set(existing));

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
        log.info(client.set(newProgram));

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

        client.addImage(
            new ImageUpdate(ImageType.PICTURE, "bla", null, new ImageLocation("http://files.vpro.nl/bril/brillen/bril.png")),
            program);
        System.out.println("Added image to " + program);


    }


    @Test
    public void addImage404() {
        assertThatThrownBy(() -> {
            String program = sampleProgram("addImage").id;

            client.addImage(
                new ImageUpdate(ImageType.PICTURE, "bla", null, new ImageLocation("http://files.vpro.nl/bril/brillen/BESTAATNIET.png")),
            program
            );
        }).isInstanceOf(ResponseError.class);

    }

    @Test
    public void testGetGroup() {
        GroupUpdate group = client.getGroup("TELEA_1051096");

        JAXB.marshal(group, System.out);

    }


    @Test
    public void testGet404() {
        GroupUpdate group = client.getGroup("bestaat niet");

        assertThat(group).isNull();

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

    @Test
    public void testCreateSegment() {
        WithId<SegmentUpdate> sample = sampleSegment("createSegment/" + System.currentTimeMillis(), null);

        System.out.println(sample.id);
    }


    @Test
    public void testUpdateSegment() {
        WithId<SegmentUpdate> sample = sampleSegment("createSegment/" + System.currentTimeMillis(), "POMS_VPRO_1424050");

        System.out.println(sample.id);
    }
    @Test
    public void testCreateProgram() {
        WithId<ProgramUpdate> sample = sampleProgram("createProgram/" + System.currentTimeMillis());

        System.out.println(sample.id);
    }


    @Test
    public void testUpdateProgram() {
        WithId<ProgramUpdate> sample = sampleProgram("createProgram");

        System.out.println(sample.id);
    }

    @Test
    public void version() {
        log.info("{} {}", client, client.getVersionNumber());
    }

    @Test
    //MSE-3604
    public void addFrame() {
        try (Response res = client.getFrameCreatorRestService().createFrame(
            "bla", Duration.ofMillis(1000),null, null, new ByteArrayInputStream("bla bla".getBytes()))) {
            log.info("{}", res);
        }
    }



    protected WithId<ProgramUpdate> sampleProgram(String test, String... broadcasters) {
        String crid = "crid://poms.omroep.nl/testcases/nl.vpro.rs.media.MediaRestClientTest/program/" + test;
        if (broadcasters.length == 0) {
            broadcasters = new String[] {"VPRO"};
        }
        MediaBuilder.ProgramBuilder program = MediaBuilder.program(ProgramType.CLIP)
            .crids(crid)
            .avType(AVType.VIDEO)
            .broadcasters(broadcasters)
            .mainTitle("Deze clip gebruiken we in een junit test " + LocalDateTime.now(Schedule.ZONE_ID).toString())
            .mainDescription(LocalDateTime.now(Schedule.ZONE_ID).toString())
            .persons(
                new Person("Pietje", "Puk", RoleType.UNDEFINED),
                new Person("Michiel", "Meeuwissen", RoleType.UNDEFINED),
                new Person("Jan", LocalDateTime.now(Schedule.ZONE_ID).toString(), RoleType.COMPOSER)
            )
            .locations(
                new Location("http://vpro.nl/bla/1", OwnerType.BROADCASTER),
                new Location("http://vpro.nl/bla/" + LocalDateTime.now(Schedule.ZONE_ID).toString(), OwnerType.BROADCASTER)
            )
            ;
        ProgramUpdate programUpdate = ProgramUpdate.create(program);
        String programMid = client.set(programUpdate);
        System.out.println("" + crid + " ->  " + programMid);
        return new WithId<>(programUpdate, programMid);

    }


    protected WithId<SegmentUpdate> sampleSegment(String test, String mid) {
        String crid = "crid://poms.omroep.nl/testcases/nl.vpro.rs.media.MediaRestClientTest/segment/" + test;
        MediaBuilder.SegmentBuilder segment = MediaBuilder.segment()
            .crids(crid)
            .mid(mid)
            .broadcasters("VPRO")
            .start(Duration.ofMillis(0))
            .avType(AVType.VIDEO)
            .mainTitle("Dit segment gebruiken we in een junit test " + LocalDateTime.now(Schedule.ZONE_ID).toString())
            .mainDescription(LocalDateTime.now(Schedule.ZONE_ID).toString())
            .persons(
                new Person("Pietje", "Puk", RoleType.UNDEFINED),
                new Person("Michiel", "Meeuwissen", RoleType.UNDEFINED),
                new Person("Jan", LocalDateTime.now(Schedule.ZONE_ID).toString(), RoleType.COMPOSER)
            )
            .locations(
                new Location("http://vpro.nl/bla/1", OwnerType.BROADCASTER),
                new Location("http://vpro.nl/bla/" + LocalDateTime.now(Schedule.ZONE_ID).toString(), OwnerType.BROADCASTER)
            )
            ;
        SegmentUpdate segmentUpdate = SegmentUpdate.create(segment);
        segmentUpdate.setMidRef("WO_VPRO_783763");
        String segmentMid = client.set(segmentUpdate);
        System.out.println("" + crid + " ->  " + segmentMid);
        return new WithId<>(segmentUpdate, segmentMid);

    }

    protected WithId<GroupUpdate> sampleGroup(String cridPostFix, String broadcaster) {
        String groupCrid = "crid://poms.omroep.nl/testcases/nl.vpro.rs.media.MediaRestClientTest/group/" + cridPostFix;
        MediaBuilder.GroupBuilder group = MediaBuilder.group(GroupType.COLLECTION)
            .crids(groupCrid)
            .avType(AVType.MIXED)
            .broadcasters(broadcaster)
            .mainTitle("Deze group gebruiken we in een junit test " + LocalDateTime.now(Schedule.ZONE_ID).toString())
            .mainDescription(LocalDateTime.now(Schedule.ZONE_ID).toString())
            ;
        GroupUpdate groupUpdate = GroupUpdate.create(group.build());
        String groupMid = client.set(groupUpdate);
        System.out.println("" + groupCrid + " ->  " + groupMid);
        return new WithId<>(groupUpdate, groupMid);

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
