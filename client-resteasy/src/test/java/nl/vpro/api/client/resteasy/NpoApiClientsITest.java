/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Date;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.api.rs.v3.page.PageRestService;
import nl.vpro.api.rs.v3.profile.ProfileRestService;
import nl.vpro.api.rs.v3.schedule.ScheduleRestService;
import nl.vpro.domain.api.ApiScheduleEvent;
import nl.vpro.domain.api.IdList;
import nl.vpro.domain.api.MultipleMediaResult;
import nl.vpro.domain.api.media.*;
import nl.vpro.domain.api.page.PageForm;
import nl.vpro.domain.api.page.PageFormBuilder;
import nl.vpro.domain.api.page.PageSearchResult;
import nl.vpro.domain.api.profile.Profile;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.Program;
import nl.vpro.domain.page.Page;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Ignore
public class NpoApiClientsITest {

    private NpoApiClients clients;

    private String target = "https://rs.poms.omroep.nl/v1/";

    //private String target = "http://localhost:8070/v1/";

    @Before
    public void setUp() throws IOException {
        clients = NpoApiClients.configured().setApiBaseUrl(target).build();
    }

    @Test(expected = NotAuthorizedException.class)
    public void testAccessForbidden() throws Exception {
        NpoApiClients wrongPassword = NpoApiClients.configured().setSecret("WRONG PASSWORD").build();

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
        InputStream response = clients.getMediaService().changes("vpro", null, 0L, null, 10, null, null);
        IOUtils.copy(response, System.out);
    }

    @Test(expected = NotFoundException.class)
    public void testChangesError() throws IOException {
        clients.getMediaService().changes("no profile", null, -1L, "ASC", 100, null, null);
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


    @Test(expected = BadRequestException.class)
    public void testBadRequest() throws Exception {
        PageRestService pageService = clients.getPageService();

        pageService.find(new PageForm(), null, "none", -1L, 1000);


    }

    @Test
    public void testGetDescendants() {
        System.out.println("" + clients.getMediaService().findDescendants(
            new MediaForm(),
            "POMS_S_VPRO_216762",
            "vpro",
            "",
            0L,
            10));
    }


    @Test
    public void testMultiple() {
        String[] mids = {"POMS_S_BNN_097259"};
        MultipleMediaResult mo = clients.getMediaService().loadMultiple(new IdList(mids), null);
        for (int i = 0; i < mids.length; i++) {
            assertThat(mo.getItems().get(i).getResult().getMid()).isEqualTo(mids[i]);
        }
    }

    @Test
    public void testSchedule() {
        ScheduleResult result = clients.getScheduleService().list(LocalDate.now(), null, null, ScheduleRestService.ASC, null, 0L, 100);
        for (ApiScheduleEvent event : result.getItems()) {
            System.out.println(event);
        }
    }

    @Test
    public void testScheduleWithDefaults() {
        ScheduleResult result = clients.getScheduleService().list(new Date(), null, null, ScheduleRestService.ASC, null, 0L, 100);
        for (ApiScheduleEvent event : result.getItems()) {
            System.out.println(event);
        }
    }


}
