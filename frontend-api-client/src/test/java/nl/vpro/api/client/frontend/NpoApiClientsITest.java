/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.frontend;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.io.ByteStreams;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.api.rs.v3.page.PageRestService;
import nl.vpro.api.rs.v3.profile.ProfileRestService;
import nl.vpro.domain.api.ApiScheduleEvent;
import nl.vpro.domain.api.IdList;
import nl.vpro.domain.api.MultipleMediaResult;
import nl.vpro.domain.api.media.*;
import nl.vpro.domain.api.page.PageForm;
import nl.vpro.domain.api.page.PageFormBuilder;
import nl.vpro.domain.api.page.PageSearchResult;
import nl.vpro.domain.api.profile.Profile;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.page.Page;
import nl.vpro.i18n.Locales;
import nl.vpro.logging.LoggerOutputStream;
import nl.vpro.util.Env;
import nl.vpro.util.Version;

import static nl.vpro.domain.api.Constants.ASC;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Slf4j
public class NpoApiClientsITest {

    private static Env env = Env.TEST;
    private NpoApiClients clients;

    @Before
    public void setUp() {
        clients = NpoApiClients.configured(env)
            .accept(MediaType.APPLICATION_XML_TYPE)
            .clearAcceptableLanguages()
            .acceptableLanguage(Locale.ENGLISH)
            .socketTimeout(Duration.ofSeconds(5))
            .connectTimeout(Duration.ofSeconds(5))
            .acceptableLanguage(Locales.DUTCH)
            .build();
        log.info("{}", clients);
    }

    @Test(expected = NotAuthorizedException.class)
    public void testAccessForbidden() {
        NpoApiClients wrongPassword = NpoApiClients
            .configured(env)
            .secret("WRONG PASSWORD")
            .build();

        MediaResult list = wrongPassword.getMediaService().list(null, null, null, null);
        log.info("{}", list);
    }

    @Test(expected = NotFoundException.class)
    public void testNotFound() {
        clients.getMediaService().load("DOES_NOT_EXIST", null, null);
    }


    @Test(expected = NotFoundException.class)
    public void testYoutubeNotFound() {
        clients.getMediaService().load("https://www.youtube.com/watch?v=YWX2PSpy1TU", null, null);
    }
    @Test
    public void testPOW_01105929() {
        clients.getMediaService().load("POW_01105929", null, null);
    }

    @Test
    public void testGetVersion() {
        String version = clients.getVersion();
        log.info(version);
        assertThat(clients.getVersion()).isNotEqualTo("unknown");
    }


    @Test
    public void testGetVersionNumber() {
        log.info("version: {}", clients.getVersionNumber());
        assertThat(clients.getVersionNumber()).isGreaterThanOrEqualTo(Version.of(4, 7));
    }

    @Test
    public void testFound() {
        for (int i = 0; i < 100; i++) {
            MediaObject program = clients.getMediaService().load("POMS_S_VPRO_788298", null, null);
            log.info(i + ":" + program.getMainTitle() + ":" + program.getMainImage().getLicense());
        }
    }

    @Test
    public void testMediaServiceLists() {
        MediaRestService mediaService = clients.getMediaService();

        MediaResult list = mediaService.list(null, null, null, null);
        assertThat(list).isNotEmpty().hasSize(10);

        String mid = list.getItems().get(1).getMid();

        MediaObject filtered = mediaService.load(mid, null, null);
        assertThat(filtered).isNotNull();
        assertThat(filtered.getTitles()).hasSize(1);

        assertThat(mediaService.listEpisodes(mid, null, null,null, null, null)).isNotNull();

        assertThat(mediaService.listMembers(mid, null, null, null, null, null)).isNotNull();

        assertThat(mediaService.listDescendants(mid, null, null, null, null, null)).isNotNull();
    }

    @Test
    public void testMediaServiceFinds() {
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
            log.error("{}", iae.getCause());
        }
        // TODO enable        assertThat(mediaService.findRelated(form, mid, null, null, null)).isNotNull();
    }

    @Test
    public void testChanges() throws IOException {
        InputStream response = clients.getMediaService().changes("vpro", null, 0L, null, null, 10, null, null, null, null);
        IOUtils.copy(response, LoggerOutputStream.info(log));
    }


    @Test
    @Ignore("Takes very long")
    public void testIterate() throws IOException {
        try (InputStream response = clients.getMediaService().iterate(new MediaForm(), "vpro-predcitions", null, 0L, Integer.MAX_VALUE, null, null)) {
            IOUtils.copy(response, ByteStreams.nullOutputStream());
        }
    }


    @Test(expected = NotFoundException.class)
    public void testChangesError() throws IOException {
        try (InputStream is = clients.getMediaService().changes("no profile", null, -1L, null, "ASC", 100, null, null, null, null)) {
            log.info("{}", is);
        }
    }


    @Test
    public void testGetPageService() {
        PageRestService pageService = clients.getPageService();
        PageForm form = PageFormBuilder.form().broadcasters("VPRO").broadcasterFacet().build();

        PageSearchResult result = pageService.find(form, null, "none", null, null);

        assertThat(result).isNotEmpty();

        Page page = result.getItems().get(0).getResult();
        log.info("sortdate: {}", page.getSortDate());
        log.info("publishstart: {}", page.getPublishStartInstant());
        log.info("creation date: {}", page.getCreationDate());

    }


    @Test
    public void testGetProfile() {
        ProfileRestService profileService = clients.getProfileService();
        Profile p = profileService.load("cultura", null);
        log.info("cultura: {}", p);

    }


    @Test(expected = BadRequestException.class)
    public void testBadRequest() {
        PageRestService pageService = clients.getPageService();
        pageService.find(new PageForm(), null, "none", -1L, 1000);
    }

    @Test
    public void testGetDescendants() {
        log.info("" + clients.getMediaService().findDescendants(
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
        MultipleMediaResult mo = clients.getMediaService().loadMultiple(new IdList(mids), null, null);
        for (int i = 0; i < mids.length; i++) {
            assertThat(mo.getItems().get(i).getResult().getMid()).isEqualTo(mids[i]);
        }
    }

    @Test
    public void testSchedule() {
        ScheduleResult result = clients.getScheduleService().list(LocalDate.now(), null, null, null, ASC, 0L, 100);
        for (ApiScheduleEvent event : result.getItems()) {
            System.out.println(event);
        }
    }

    @Test
    public void testScheduleWithDefaults() {
        ScheduleResult result = clients.getScheduleService().list(LocalDate.now(), null, null, null, ASC, 0L, 100);
        for (ApiScheduleEvent event : result.getItems()) {
            System.out.println(event);
        }
    }

    @Test
    public void testInfiniteTimeout() {
        Instant start = Instant.now();
        String url = "https://httpbin.org/delay/10";
        ClientHttpEngine httpClient = clients.getClientHttpEngineNoTimeout();
        ResteasyClientBuilder builder = new ResteasyClientBuilder().httpEngine(httpClient);
        Response response = builder.build().target(url).request().get();
        Duration duration = Duration.between(start, Instant.now());
        assertThat(duration.getSeconds()).isGreaterThanOrEqualTo(10);
        assertThat(response.getStatus()).isEqualTo(200);
        System.out.println(response.readEntity(String.class));
    }

    @Test
    public void testInfiniteTimeoutSocket() {
        Instant start = Instant.now();
        String url = "https://httpbin.org/drip?duration=12&numbytes=5&code=200";
        ClientHttpEngine httpClient = clients.getClientHttpEngineNoTimeout();
        ResteasyClientBuilder builder = new ResteasyClientBuilder().httpEngine(httpClient);
        Response response = builder.build().target(url).request().get();
        assertThat(response.getStatus()).isEqualTo(200);
        log.info(response.readEntity(String.class));
        Duration duration = Duration.between(start, Instant.now());
        assertThat(duration.getSeconds()).isGreaterThanOrEqualTo(10);

    }


    @Test(expected = javax.ws.rs.ProcessingException.class)
    public void testTimeout() {
        String url = "https://httpbin.org/delay/11";
        ClientHttpEngine httpClient = clients.getClientHttpEngine();
        ResteasyClientBuilder builder = new ResteasyClientBuilder()
            .httpEngine(httpClient);
        Response response = builder.build().target(url).request().get();
        log.info("{}", response);
    }


    @Test(expected = javax.ws.rs.ProcessingException.class)
    public void testTimeoutSocket() {
        String url = "https://httpbin.org/drip?duration=12&numbytes=5&code=200";
        clients.setSocketTimeout("PT0.01S");
        ClientHttpEngine httpClient = clients.getClientHttpEngine();
        ResteasyClientBuilder builder = new ResteasyClientBuilder().httpEngine(httpClient);
        Response response = builder.build().target(url).request().get();
        assertThat(response.getStatus()).isEqualTo(200);
        log.info(response.readEntity(String.class));
    }




}
