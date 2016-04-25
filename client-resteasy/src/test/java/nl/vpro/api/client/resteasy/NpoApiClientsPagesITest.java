/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.domain.api.FacetOrder;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.SearchResultItem;
import nl.vpro.domain.api.page.*;
import nl.vpro.domain.page.*;
import nl.vpro.jackson2.Jackson2Mapper;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 4.3
 */
//@Ignore
public class NpoApiClientsPagesITest {

    private static final Logger LOG = LoggerFactory.getLogger(NpoApiClientsPagesITest.class);

    private static NpoApiClients clients;

    @BeforeClass
    public static void setUp() throws IOException {
        clients = NpoApiClients.configured().build();
        System.out.println("Testing with " + clients);
    }

    @Test
    public void testGenres() throws JsonProcessingException {
        PageForm form =
            PageFormBuilder.form()
            .addGenres("3.0.1.7.21")
            .build();
        LOG.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

        PageSearchResult result = clients.getPageService().find(form, null, null, 0L, 10);

        assertThat(result.asList()).hasSize(10);
        assertThat(result.iterator().next().getResult().getGenres()).contains(Genre.of("3.0.1.7.21"));

    }

    @Test
    public void testProfile() throws IOException {
        PageForm form =
            PageFormBuilder.form()
                .addGenres("3.0.1.7.21")
                .build();
        LOG.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));
        PageSearchResult result = clients.getPageService().find(form, "vpro", null, 0L, 10);

        assertThat(result.asList()).hasSize(10);
        assertThat(result.iterator().next().getResult().getGenres()).contains(Genre.of("3.0.1.7.21"));
        assertThat(result.iterator().next().getResult().getPortal().getId()).isEqualTo("VPRONL");

    }

    @Test
    public void testPortal() throws JsonProcessingException {
        PageForm form =
            PageFormBuilder.form()
                .addPortals("VPRONL")
                .build();
        LOG.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

        PageSearchResult result = clients.getPageService().find(form, null, null, 0L, 10);

        assertThat(result.asList()).hasSize(10);
        assertThat(result.iterator().next().getResult().getPortal().getId()).isEqualTo("VPRONL");

    }

    @Test
    public void testSections() throws JsonProcessingException {
        PageForm form =
            PageFormBuilder.form()
                .addSections("VPRONL./vpronl/jeugd")
                .build();
        LOG.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

        PageSearchResult result = clients.getPageService().find(form, null, null, 0L, 10);

        assertThat(result.asList()).hasSize(10);
        assertThat(result.iterator().next().getResult().getPortal().getId()).isEqualTo("VPRONL");
        assertThat(result.iterator().next().getResult().getPortal().getSection().getPath()).isEqualTo("/vpronl/jeugd");


    }


    @Test
    public void testPageType() throws JsonProcessingException {
        PageForm form =
            PageFormBuilder.form()
                .addTypes(PageType.ARTICLE)
                .build();
        LOG.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

        PageSearchResult result = clients.getPageService().find(form, null, null, 0L, 10);

        assertThat(result.asList()).hasSize(10);
        for (SearchResultItem<? extends Page> r : result) {
            assertThat(r.getResult().getType()).isEqualTo(PageType.ARTICLE);
        }

    }

    @Test
    public void testGenresFacets() throws JsonProcessingException {
        PageForm form =
            PageFormBuilder.form()
                .text("test")
                .genreFacet()
                .build();
        LOG.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

        PageSearchResult result = clients.getPageService().find(form, "woord", null, 0L, 0);

        assertThat(result.asList()).hasSize(0);
        for (GenreFacetResultItem gfc : result.getFacets().getGenres()) {
            assertThat(gfc.getCount()).isGreaterThan(0);
            assertThat(gfc.getTerms().get(0).getName()).doesNotContain(".");
            System.out.println(gfc.getCount() + " " + gfc.getTerms().get(0).getName() + " " + gfc.getId());
            assertThat(gfc.getId()).startsWith("3.0.4.");
        }
    }

    @Test
    public void testNPA186() throws IOException {
        PageForm form =
            PageFormBuilder.form()
                .text("Café 'de vogel'")
                .build();
        LOG.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));
        PageSearchResult result = clients.getPageService().find(form, null, null, 0L, 10);

        assertThat(result.asList()).hasSize(10);

        for (SearchResultItem<? extends Page> r : result) {
            String s = Jackson2Mapper.getInstance().writeValueAsString(r.getResult());
            assertThat(s.toLowerCase().contains("vogel") || s.toLowerCase().contains("café")).isTrue();
        }


    }

    @Test
    public void testNPA188() throws IOException {
        PageForm form =
            PageFormBuilder.form()
                .genres("3.0.1.8.30")
                .genreFacet(new PageSearchableTermFacet(null, FacetOrder.COUNT_DESC, 100))
                .build();
        LOG.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

        PageSearchResult result = clients.getPageService().find(form, null, null, 0L, 10);

        assertThat(result.asList()).hasSize(10);


        for (SearchResultItem<? extends Page> r : result) {
            assertThat(r.getResult().getGenres()).contains(Genre.of("3.0.1.8.30"));
        }
        for (GenreFacetResultItem gfc : result.getFacets().getGenres()) {
            assertThat(gfc.getCount()).isGreaterThan(0);
            assertThat(gfc.getTerms().get(0).getName()).doesNotContain(".");
            System.out.println(gfc.getCount() + " " + gfc.getTerms().get(0).getName() + " " + gfc.getId());
        }


    }

    @Test
    public void testNPA193() throws JsonProcessingException {
        PageForm form =
            PageFormBuilder.form()
            .text("'groot promotie blaas quintet'")
            .build();
        LOG.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

        PageSearchResult result = clients.getPageService().find(form, null, null, 0L, 5);

        assertThat(result.asList().size()).isGreaterThan(0);


        for (SearchResultItem<? extends Page> r : result) {
            String s = Jackson2Mapper.getInstance().writeValueAsString(r.getResult());
            assertThat(s).containsIgnoringCase("groot promotie blaas quintet");
        }

    }

    @Test
    public void testRelations() throws JsonProcessingException {
        RelationDefinition DIRECTOR  = RelationDefinition.of("CINEMA_DIRECTOR", "VPRO");
        PageForm form =
            PageFormBuilder.form()
                .relation(DIRECTOR, "Stanley Kubrick", null)
                .build();
        LOG.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

        PageSearchResult result = clients.getPageService().find(form, null, null, 0L, 5);


        for (SearchResultItem<? extends Page> r : result) {
            assertThat(r.getResult().getRelations()).contains(Relation.text(DIRECTOR, "Stanley Kubrick"));
        }
    }


    @Test
    public void testSortLastModified() throws JsonProcessingException {
       PageForm form =
            PageFormBuilder.form()
                .addSortField("lastModified", Order.ASC)
                .build();
        LOG.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

        PageSearchResult result = clients.getPageService().find(form, null, null, 0L, 5);

        for (SearchResultItem<? extends Page> r : result) {
            System.out.println(r.getResult().getLastModified());
        }
    }
/*
    @Test
    public void testSortDate() {
        RelationDefinition DIRECTOR = RelationDefinition.of("CINEMA_DIRECTOR", "VPRO");

        PageSearchResult result = clients.getPageService().find(form, null, null, 0L, 5);


        for (SearchResultItem<? extends Page> r : result) {
            assertThat(r.getResult().getRelations()).contains(Relation.text(DIRECTOR, "Stanley Kubrick"));
        }
    }*/
}
