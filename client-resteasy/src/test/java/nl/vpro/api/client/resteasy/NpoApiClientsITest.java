/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.api.rs.v3.page.PageRestService;
import nl.vpro.domain.api.MediaResult;
import nl.vpro.domain.api.MediaSearchResult;
import nl.vpro.domain.api.PageSearchResult;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.MediaFormBuilder;
import nl.vpro.domain.api.page.PageForm;
import nl.vpro.domain.api.page.PageFormBuilder;
import nl.vpro.domain.media.MediaObject;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class NpoApiClientsITest {

    private NpoApiClients clients;

    @Before
    public void setUp() {
        clients = new NpoApiClients(
            "http://rs-dev.poms.omroep.nl/v1/",
            "ione7ahfij",
            "***REMOVED***",
            "http://www.vpro.nl"
        );
    }

    @Test(expected = NotAuthorizedException.class)
    public void testAccessForbidden() throws Exception {
        clients = new NpoApiClients(
            "http://rs-dev.poms.omroep.nl/v1/api/",
            "ione7ahfij",
            "WRONG_PASSWORD",
            "http://www.vpro.nl"
        );

        clients.getMediaService().list(null, null, null, null);
    }

    @Test(expected = BadRequestException.class)
    public void testNotFound() throws Exception {
        clients.getMediaService().load("DOES_NOT_EXIST", null);
    }

    @Test
    public void testMediaServiceLists() throws Exception {
        MediaRestService mediaService = clients.getMediaService();

        MediaResult list = mediaService.list(null, null, null, null);
        assertThat(list).isNotEmpty().hasSize(10);

        String mid = list.getItems().get(0).getMid();

        MediaObject filtered = mediaService.load(mid, "none");
        assertThat(filtered).isNotNull();
        assertThat(filtered.getTitles()).hasSize(1);

        assertThat(mediaService.listEpisodes(mid, null, null, null, null)).isNotNull();

        assertThat(mediaService.listMembers(mid, null, null, null, null)).isNotNull();

        assertThat(mediaService.listDescendants(mid, null, null, null, null)).isNotNull();
    }

    @Test
    public void testMediaServiceFinds() throws Exception {
        MediaRestService mediaService = clients.getMediaService();
        MediaForm form = MediaFormBuilder.form().broadcasters("VPRO").broadcasterFacet().build();

        MediaSearchResult list = mediaService.find(form, null, "none", null, null);
        assertThat(list).isNotEmpty().hasSize(10);
        assertThat(list.getFacets().getBroadcasters()).isNotEmpty();

        String mid = list.getItems().get(0).getResult().getMid();

        assertThat(mediaService.findEpisodes(form, mid, null, null, null, null)).isNotNull();

        assertThat(mediaService.findMembers(form, mid, null, null, null, null)).isNotNull();

        assertThat(mediaService.findDescendants(form, mid, null, null, null, null)).isNotNull();

// TODO enable        assertThat(mediaService.findRelated(form, mid, null, null, null)).isNotNull();
    }

    @Test
    public void testGetPageService() throws Exception {
        PageRestService pageService = clients.getPageService();
        PageForm form = PageFormBuilder.form().broadcasters("VPRO").broadcasterFacet().build();

        PageSearchResult result = pageService.find(form, null, "none", null, null);

        assertThat(result).isNotEmpty();
    }
}
