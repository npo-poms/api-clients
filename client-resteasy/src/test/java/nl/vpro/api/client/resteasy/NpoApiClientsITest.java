/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.api.rs.v3.page.PageRestService;
import nl.vpro.api.rs.v3.profile.ProfileRestService;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.MediaFormBuilder;
import nl.vpro.domain.api.media.MediaResult;
import nl.vpro.domain.api.media.MediaSearchResult;
import nl.vpro.domain.api.page.PageForm;
import nl.vpro.domain.api.page.PageFormBuilder;
import nl.vpro.domain.api.page.PageSearchResult;
import nl.vpro.domain.api.profile.Profile;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.Program;
import nl.vpro.domain.page.Page;
import nl.vpro.jackson2.Jackson2Mapper;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Ignore
public class NpoApiClientsITest {

    private NpoApiClients clients;

    //private String target = "http://rs-dev.poms.omroep.nl/v1/";

    private String target = "http://localhost:8070/v1/";

    @Before
    public void setUp() {
        clients = new NpoApiClients(
            target,
            "ione7ahfij",
            "***REMOVED***",
            "http://www.vpro.nl",
            10000
        );
    }

    @Test(expected = NotAuthorizedException.class)
    public void testAccessForbidden() throws Exception {
        NpoApiClients wrongPassword = new NpoApiClients(
            target,
            "ione7ahfij",
            "WRONG_PASSWORD",
            "http://www.vpro.nl",
            10000
        );

        wrongPassword.getMediaService().list(null, null, null, null);
    }

    @Test(expected = NotFoundException.class)
    public void testNotFound() throws Exception {
        clients.getMediaService().load("DOES_NOT_EXIST", null);
    }




    @Test
	public void testFound() throws Exception {
		Program program = (Program) clients.getMediaService().load("POMS_VPRO_158299", null);
		System.out.println(program.getMainTitle());
	}

    @Test
    public void testMediaServiceLists() throws Exception {
        MediaRestService mediaService = clients.getMediaService();

        MediaResult list = mediaService.list(null, null, null, null);
        assertThat(list).isNotEmpty().hasSize(10);

        String mid = list.getItems().get(1).getMid();

        MediaObject filtered = mediaService.load(mid, null);
        assertThat(filtered).isNotNull();
        assertThat(filtered.getTitles()).hasSize(1);

        assertThat(mediaService.listEpisodes(mid, null, null, null, null)).isNotNull();

        assertThat(mediaService.listMembers(mid, null, null, null, null)).isNotNull();

        assertThat(mediaService.listDescendants(mid, null, null, null, null)).isNotNull();
    }

    @Test
    public void testMediaServiceFinds() throws Exception {
        try {
            MediaRestService mediaService = clients.getMediaService();
            MediaForm form = MediaFormBuilder.form().broadcasters("VPRO").broadcasterFacet().build();

            MediaSearchResult list = mediaService.find(form, null, "none", null, null);
            assertThat(list).isNotEmpty().hasSize(10);
            assertThat(list.getFacets().getBroadcasters()).isNotEmpty();

            String mid = list.getItems().get(0).getResult().getMid();

            assertThat(mediaService.findEpisodes(form, mid, null, null, null, null)).isNotNull();

            assertThat(mediaService.findMembers(form, mid, null, null, null, null)).isNotNull();

            assertThat(mediaService.findDescendants(form, mid, null, null, null, null)).isNotNull();
        } catch (InternalServerErrorException iae) {
            System.out.print(iae.getCause());
        }
        // TODO enable        assertThat(mediaService.findRelated(form, mid, null, null, null)).isNotNull();
    }

    @Test
    public void testChanges() throws IOException {
        InputStream response = clients.getMediaService().changes("vpro", null, 0l, null, 10, null, null);
        IOUtils.copy(response, System.out);
    }

    @Test(expected = NotFoundException.class)
    public void testChangesError() throws IOException {
        clients.getMediaService().changes("no profile", null, -1l, "ASC", 100, null, null);
    }


    @Test
    public void testGetPageService() throws Exception {
        PageRestService pageService = clients.getPageService();
        PageForm form = PageFormBuilder.form().broadcasters("VPRO").broadcasterFacet().build();

        PageSearchResult result = pageService.find(form, null, "none", null, null);

        assertThat(result).isNotEmpty();

        Page page = result.getItems().get(0).getResult();
        System.out.println(page.getSortDate());
        System.out.println(page.getPublishStart());
        System.out.println(page.getCreationDate());

    }


    @Test
    public void testGetProfile() throws Exception {
        ProfileRestService profileService = clients.getProfileService();
        Profile p = profileService.load("cultura", null);

        System.out.println(p);

    }

    @Test
    public void testSearchPage() throws Exception {
        PageRestService pageService = clients.getPageService();

        PageForm form = Jackson2Mapper.getInstance().readValue("{\"searches\":{\"types\":[\"PLAYER\"]},\"sort\":{\"sortDate\":\"DESC\"},\"facets\":{\"keywords\":{\"threshold\":0,\"sort\":\"COUNT_DESC\",\"offset\":0,\"max\":24},\"genres\":{\"threshold\":0,\"sort\":\"COUNT_DESC\",\"offset\":0,\"max\":24},\"sections\":{\"threshold\":0,\"sort\":\"COUNT_DESC\",\"offset\":0,\"max\":24}},\"mediaForm\":{\"facets\":{\"avTypes\":{\"threshold\":0,\"sort\":\"COUNT_DESC\",\"offset\":0,\"max\":24}},\"highlight\":false},\"highlight\":false}", PageForm.class);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        JAXB.marshal(form, out);

        PageForm validated = (PageForm) getUnmarshaller(PageForm.class).unmarshal(new StringReader(out.toString()));
        System.out.println(out.toString());

        PageSearchResult result = pageService.find(validated, "wetenschap", null, 0l, 10);

        assertThat(result).isNotEmpty();

        Page page = result.getItems().get(0).getResult();
        System.out.println(page.getSortDate());
        System.out.println(page.getPublishStart());
        System.out.println(page.getCreationDate());



    }

    private Unmarshaller getUnmarshaller(Class clazz) throws JAXBException, IOException, SAXException {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        JAXBContext context = JAXBContext.newInstance(clazz);
        final DOMResult[] result = new DOMResult[1];
        result[0] = new DOMResult();
        context.generateSchema(new SchemaOutputResolver() {
            @Override
            public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
                result[0].setSystemId(namespaceUri);
                return result[0];
            }
        });
        Schema schema = sf.newSchema(new DOMSource(result[0].getNode()));
        Unmarshaller unmarshaller = JAXBContext.newInstance(PageForm.class).createUnmarshaller();
        unmarshaller.setSchema(schema);
        return unmarshaller;
    }

}
