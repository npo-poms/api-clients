/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.frontend;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.*;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import nl.vpro.api.client.resteasy.ResteasyHelper;
import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.api.rs.v3.page.PageRestService;
import nl.vpro.api.rs.v3.profile.ProfileRestService;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.media.*;
import nl.vpro.domain.api.page.*;
import nl.vpro.domain.api.profile.Profile;
import nl.vpro.domain.api.profile.exception.ProfileNotFoundException;
import nl.vpro.domain.classification.ClassificationServiceLocator;
import nl.vpro.domain.media.*;
import nl.vpro.domain.page.Page;
import nl.vpro.i18n.Locales;
import nl.vpro.logging.LoggerOutputStream;
import nl.vpro.test.jupiter.TimingExtension;
import nl.vpro.util.Env;
import nl.vpro.util.Version;

import static nl.vpro.domain.api.Constants.ASC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 *
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@SuppressWarnings({"resource", "DataFlowIssue"})
@Slf4j
@ExtendWith(TimingExtension.class)
class NpoApiClientsITest {

    private static Env env = Env.TEST;
    private NpoApiClients clients;

    @BeforeEach
    void setUp() {
        ClassificationServiceLocator.setInstance(MediaClassificationService.getInstance());

        clients = NpoApiClients.configured(env)
            .accept(MediaType.APPLICATION_XML_TYPE)
            .clearAcceptableLanguages()
            .acceptableLanguage(Locale.ENGLISH)
            .socketTimeout(Duration.ofSeconds(15))
            .connectTimeout(Duration.ofSeconds(15))
            .acceptableLanguage(Locales.DUTCH)
            .build();
        log.info("{}", clients);
    }

    @Test
    void testAccessForbidden() {
        assertThatThrownBy(() -> {
            NpoApiClients wrongPassword = NpoApiClients
                .configured(env)
                .secret("WRONG PASSWORD")
                .build();

            MediaResult list = wrongPassword.getMediaService().list(null, null, null, null);
            log.info("{}", list);
        }).isInstanceOf(NotAuthorizedException.class);
    }

    @Test
    void testNotFound() {
        assertThatThrownBy(() -> {
            clients.getMediaService().load("DOES_NOT_EXIST", null, null);
        }).isInstanceOf(NotFoundException.class);
    }


    @Test
    void testYoutubeNotFound() {
        assertThatThrownBy(() -> {

            clients.getMediaService().load("https://www.youtube.com/watch?v=YWX2PSpy1TU", null, null);
        }).isInstanceOf(NotFoundException.class);

    }
    @Test
    void testPOW_01105929() {
        clients.getMediaService().load("POW_01105929", null, null);
    }

    @Test
    void testGetVersion() {
        String version = clients.getVersion();
        log.info(version);
        assertThat(clients.getVersion()).isNotEqualTo("unknown");
    }


    @Test
    void testGetVersionNumber() {
        log.info("version: {}", clients.getVersionNumber());
        assertThat(clients.getVersionNumber()).isGreaterThanOrEqualTo(Version.of(4, 7));
    }

    @Test
    void testFound() {
        for (int i = 0; i < 100; i++) {
            MediaObject program = clients.getMediaService().load("POMS_S_VPRO_788298", null, null);
            log.info(i + ":" + program.getMainTitle() + ":" + program.getMainImage().getLicense());
        }
    }

    @Test
    void testMediaServiceLists() throws ProfileNotFoundException {
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
    void testMediaServiceFinds() throws ProfileNotFoundException {
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
            log.error("{}", iae.getMessage(), iae.getCause());
        }
        // TODO enable        assertThat(mediaService.findRelated(form, mid, null, null, null)).isNotNull();
    }

    @Test
    void testChanges() throws IOException, ProfileNotFoundException {
        try (InputStream response = clients.getMediaService().changes("vpro", null, 0L, null, null, 10, null, null , null).readEntity(InputStream.class)) {
            IOUtils.copy(response, LoggerOutputStream.info(log));
        }
    }


    @Test
    @Disabled("Takes very long")
    void testIterate() throws IOException, ProfileNotFoundException {
        try (InputStream response = clients.getMediaService()
            .iterate(new MediaForm(), null, null, 0L, Integer.MAX_VALUE)
            .readEntity(InputStream.class)) {
            IOUtils.copyLarge(response,
                new GZIPOutputStream(Files.newOutputStream(Paths.get("/tmp/test.full.json.gz")))
            );
        }
    }



    @Test
    void testChangesError() {
        assertThatThrownBy(() -> {

            try (Response rest = clients.getMediaService().changes("no profile", null, -1L, null, Order.ASC, 100, null, null, null);
                 InputStream is = rest.readEntity(InputStream.class)) {
                log.info("{}", is);
            }
        }).isInstanceOf(NotFoundException.class);
    }


    @Test
    void testGetPageService() {
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
    void testGetProfile() {
        ProfileRestService profileService = clients.getProfileService();
        Profile p = profileService.load("cultura");
        log.info("cultura: {}", p);

    }


    @Test
    void testBadRequest() {
        assertThatThrownBy(() -> {

            PageRestService pageService = clients.getPageService();
            pageService.find(new PageForm(), null, "none", -1L, 1000);
        }).isInstanceOf(BadRequestException.class);
    }

    @Test
    void testGetDescendants() throws ProfileNotFoundException {
        log.info(String.valueOf(clients.getMediaService().findDescendants(
            new MediaForm(),
            "POMS_S_VPRO_216762",
            "vpro",
            "",
            0L,
            10)));
    }


    @Test
    void testMultiple() throws ProfileNotFoundException {
        String[] mids = {"POMS_S_BNN_097259"};
        MultipleMediaResult mo = clients.getMediaService().loadMultiple(new IdList(mids), null, null);
        for (int i = 0; i < mids.length; i++) {
            assertThat(mo.getItems().get(i).getResult().getMid()).isEqualTo(mids[i]);
        }
    }

    @Test
    void testSchedule() {
        ScheduleResult result = clients.getScheduleService().list(LocalDate.now(), null, null, null, ASC, 0L, 100);
        for (ApiScheduleEvent event : result.getItems()) {
            System.out.println(event);
        }
    }

    @Test
    void testScheduleWithDefaults() {
        ScheduleResult result = clients.getScheduleService().list(LocalDate.now(), null, null, null, ASC, 0L, 100);
        for (ApiScheduleEvent event : result.getItems()) {
            System.out.println(event);
        }
    }

    @Test
    void testInfiniteTimeout() {
        Instant start = Instant.now();
        String url = "https://httpbin.org/delay/10";
        ClientHttpEngine httpClient = clients.getClientHttpEngineNoTimeout();
        ResteasyClientBuilder builder = ResteasyHelper.clientBuilder().httpEngine(httpClient);
        Response response = builder.build().target(url).request().get();
        Duration duration = Duration.between(start, Instant.now());
        assertThat(duration.getSeconds()).isGreaterThanOrEqualTo(10);
        assertThat(response.getStatus()).isEqualTo(200);
        System.out.println(response.readEntity(String.class));
    }

    @Test
    void testInfiniteTimeoutSocket() {
        Instant start = Instant.now();
        String url = "https://httpbin.org/drip?duration=5&numbytes=5&code=200";
        ClientHttpEngine httpClient = clients.getClientHttpEngineNoTimeout();
        ResteasyClientBuilder builder = ResteasyHelper.clientBuilder().httpEngine(httpClient);
        Response response = builder.build().target(url).request().get();
        assertThat(response.getStatus()).isEqualTo(200);
        log.info(response.readEntity(String.class));
        Duration duration = Duration.between(start, Instant.now());
        assertThat(duration.toMillis()).isGreaterThanOrEqualTo(4000);

    }


    @Test
    void testTimeout() {
        assertThatThrownBy(() -> {
            String url = "https://httpbin.org/delay/11";
            ClientHttpEngine httpClient = clients.getClientHttpEngine();
            ResteasyClientBuilder builder = ResteasyHelper.clientBuilder()
                .httpEngine(httpClient);
            Response response = builder.build().target(url).request().get();
            log.info("{}", response);
        }).isInstanceOf(jakarta.ws.rs.ProcessingException.class);
    }


    @Test
    void testTimeoutSocket() {
        assertThatThrownBy(() -> {
            String url = "https://httpbin.org/drip?duration=12&numbytes=5&code=200";
            clients.setSocketTimeout("PT0.01S");
            ClientHttpEngine httpClient = clients.getClientHttpEngine();
            ResteasyClientBuilder builder = ResteasyHelper.clientBuilder().httpEngine(httpClient);
            Response response = builder.build().target(url).request().get();
            assertThat(response.getStatus()).isEqualTo(200);
            log.info(response.readEntity(String.class));
        }).isInstanceOf(jakarta.ws.rs.ProcessingException.class);

    }

    @Test
    void thesaurus() throws IOException {
        Instant start = LocalDateTime.of(2020, 1, 1, 0, 0).atZone(Schedule.ZONE_ID).toInstant();
        try (Response response = clients.getThesaurusRestService().listConceptUpdates(start, start.plus(Duration.ofDays(30)))) {
            InputStream ia = response.readEntity(InputStream.class);
            IOUtils.copy(ia, System.out);
        }

    }


    @Test
    void listForAncestor() {
        ScheduleResult vpro = clients.getScheduleService().listForAncestor("some mid", null, Instant.now(), Instant.now().plus(Duration.ofDays(7)), null, null, 0, 100);
        log.info("{}, ", vpro);


    }

    @Test
    void list() {
        ScheduleResult vpro = clients.getScheduleService().list(LocalDate.now(), null, null, null, null, 0, 100);
        log.info("{}, ", vpro);


    }




}
