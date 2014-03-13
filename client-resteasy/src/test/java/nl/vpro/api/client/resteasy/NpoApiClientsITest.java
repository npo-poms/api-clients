/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import org.junit.Before;
import org.junit.Test;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.domain.api.MediaResult;
import nl.vpro.domain.media.MediaObject;

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
            "http://rs-dev.poms.omroep.nl/v1/api/",
//            "http://localhost:8080/v1/api/",
            "ione7ahfij",
            "***REMOVED***",
            "http://www.vpro.nl"
        );
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
    public void testGetPageService() throws Exception {

    }
}
