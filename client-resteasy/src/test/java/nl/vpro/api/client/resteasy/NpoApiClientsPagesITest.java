/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.domain.api.FacetOrder;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.SearchResultItem;
import nl.vpro.domain.api.page.*;
import nl.vpro.domain.media.Schedule;
import nl.vpro.domain.page.*;
import nl.vpro.jackson2.Jackson2Mapper;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 4.3
 */
@Slf4j
public class NpoApiClientsPagesITest {

    private static NpoApiClients clients;

    private static NpoApiClients clientsShortTimeouts;


    @BeforeClass
    public static void setUp() throws IOException {
        clients = NpoApiClients
            .configured()
            .build();

        clientsShortTimeouts = NpoApiClients
            .configured()
            .socketTimeout(Duration.ofMillis(100))
            .connectTimeout(Duration.ofMillis(1000))
            .build();
        System.out.println("Testing with " + clients);
    }

    @Test
    public void testGenres() throws JsonProcessingException {
        PageForm form =
            PageFormBuilder.form()
            .addGenres("3.0.1.7.21")
            .build();
        log.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

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
        log.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));
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
        log.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

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
        log.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

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
        log.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

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
        log.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

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
        log.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));
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
        log.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

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
        log.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

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
        log.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

        PageSearchResult result = clients.getPageService().find(form, null, null, 0L, 5);


        for (SearchResultItem<? extends Page> r : result) {
            assertThat(r.getResult().getRelations()).contains(Relation.text(DIRECTOR, "Stanley Kubrick"));
        }
    }


    @Test
    public void testSortDate() throws JsonProcessingException {
       PageForm form =
            PageFormBuilder.form()
                .addSortField(PageSortField.sortDate, Order.ASC)
                .build();
        log.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

        PageSearchResult result = clients.getPageService().find(form, null, null, 0L, 5);
        Instant prev = Instant.MIN;
        for (SearchResultItem<? extends Page> r : result) {
            Instant sortDate = r.getResult().getSortDate();
            System.out.println(sortDate);
            assertThat(prev.isBefore(sortDate) || prev.equals(sortDate)).isTrue();
            prev = sortDate;
        }
    }


    @Test
    public void testLastModified() throws JsonProcessingException {
        String[] profiles = {null, "cultura", "woord", "vpro"};

        for (String profile : profiles) {
            PageForm form =
                PageFormBuilder.form()
                    .addSortField(PageSortField.lastModified, Order.DESC)
                    .build();
            log.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

            PageSearchResult result = clients.getPageService().find(form, profile, null, 0L, 5);
            Instant prev = Instant.MAX;
            for (SearchResultItem<? extends Page> r : result) {
                Instant lastModified = r.getResult().getLastModified();
                if (lastModified == null){
                    log.warn(r.getResult() + ": NULL");
                    continue;
                }
                log.info(r.getResult().getUrl() + ":" + lastModified.atZone(Schedule.ZONE_ID));
                assertThat(prev.isAfter(lastModified) || prev.equals(lastModified)).isTrue();
                prev = lastModified;
            }
        }
    }

    @Test
    public void testMGNL_14570() throws IOException {
        String largeForm = "{\"mediaForm\":{\"searches\":{\"mediaIds\":[{\"value\":\"POW_00794396\",\"match\":\"should\"},{\"value\":\"POW_01037980\",\"match\":\"should\"},{\"value\":\"POW_00906560\",\"match\":\"should\"},{\"value\":\"POW_00794399\",\"match\":\"should\"},{\"value\":\"POW_03332982\",\"match\":\"should\"},{\"value\":\"POW_00794401\",\"match\":\"should\"},{\"value\":\"POW_00794402\",\"match\":\"should\"},{\"value\":\"POW_03068576\",\"match\":\"should\"},{\"value\":\"POW_03068580\",\"match\":\"should\"},{\"value\":\"POW_03068584\",\"match\":\"should\"},{\"value\":\"POW_03068589\",\"match\":\"should\"},{\"value\":\"AUTO_NIEUWS\",\"match\":\"should\"},{\"value\":\"AUTO_DEZWARTELIJST\",\"match\":\"should\"},{\"value\":\"AUTO_ECHTJASPER\",\"match\":\"should\"},{\"value\":\"AUTO_NACHTVANDEVLUCHTELING\",\"match\":\"should\"},{\"value\":\"AUTO_TOP\",\"match\":\"should\"},{\"value\":\"AUTO_GRANDCAFANTWERPEN\",\"match\":\"should\"},{\"value\":\"AUTO_GRANDCAFKRANENBARG\",\"match\":\"should\"},{\"value\":\"AUTO_SPIJKERSMETKOPPEN\",\"match\":\"should\"},{\"value\":\"AUTO_TWEEAANZEE\",\"match\":\"should\"},{\"value\":\"AUTO_ZOMERSPIJKERS\",\"match\":\"should\"},{\"value\":\"AUTO_METSANDERDEHEER\",\"match\":\"should\"},{\"value\":\"AUTO_MUZIEKCAF\",\"match\":\"should\"},{\"value\":\"AUTO_BESTOFNIGHTMETHANS\",\"match\":\"should\"},{\"value\":\"AUTO_NORTHSEAJAZZFESTIVAL\",\"match\":\"should\"},{\"value\":\"AUTO_THEBESTOFNIGHT\",\"match\":\"should\"},{\"value\":\"AUTO_CORNKLIJNSSOULSENSATIONS\",\"match\":\"should\"},{\"value\":\"AUTO_EKDOMSFUNKYWEEKENDTRIP\",\"match\":\"should\"},{\"value\":\"AUTO_EKDOMSFUNNYWEEKENDTRIP\",\"match\":\"should\"},{\"value\":\"AUTO_ONTHEBEAT\",\"match\":\"should\"},{\"value\":\"AUTO_LICENCECHILL\",\"match\":\"should\"},{\"value\":\"AUTO_LICENCETOCHILL\",\"match\":\"should\"},{\"value\":\"T_POW_03430156\",\"match\":\"should\"},{\"value\":\"T_POW_03427433\",\"match\":\"should\"},{\"value\":\"T_POW_03430184\",\"match\":\"should\"},{\"value\":\"T_POW_03430186\",\"match\":\"should\"},{\"value\":\"T_POW_03430187\",\"match\":\"should\"},{\"value\":\"T_POW_03420924\",\"match\":\"should\"},{\"value\":\"T_POW_03428750\",\"match\":\"should\"},{\"value\":\"T_POW_03428757\",\"match\":\"should\"},{\"value\":\"T_POW_03428770\",\"match\":\"should\"},{\"value\":\"T_POW_03428772\",\"match\":\"should\"},{\"value\":\"T_POW_03428752\",\"match\":\"should\"},{\"value\":\"T_POW_03428759\",\"match\":\"should\"},{\"value\":\"T_POW_03428771\",\"match\":\"should\"},{\"value\":\"T_POW_03440838\",\"match\":\"should\"},{\"value\":\"T_POW_03428768\",\"match\":\"should\"},{\"value\":\"T_POW_03430177\",\"match\":\"should\"},{\"value\":\"T_POW_03430172\",\"match\":\"should\"},{\"value\":\"T_POW_03430173\",\"match\":\"should\"},{\"value\":\"T_POW_03421097\",\"match\":\"should\"},{\"value\":\"T_POW_03421203\",\"match\":\"should\"},{\"value\":\"T_POW_03421424\",\"match\":\"should\"},{\"value\":\"POW_03099638\",\"match\":\"should\"},{\"value\":\"POW_03015072\",\"match\":\"should\"},{\"value\":\"POW_01013980\",\"match\":\"should\"},{\"value\":\"POW_03079198\",\"match\":\"should\"},{\"value\":\"POW_02772004\",\"match\":\"should\"},{\"value\":\"POW_00797500\",\"match\":\"should\"},{\"value\":\"POW_00824940\",\"match\":\"should\"},{\"value\":\"POW_00797462\",\"match\":\"should\"},{\"value\":\"POW_03079199\",\"match\":\"should\"},{\"value\":\"POW_03079238\",\"match\":\"should\"},{\"value\":\"POW_00797505\",\"match\":\"should\"},{\"value\":\"POW_03079239\",\"match\":\"should\"},{\"value\":\"POW_00793376\",\"match\":\"should\"},{\"value\":\"POW_03164778\",\"match\":\"should\"},{\"value\":\"AUTO_EENGOEDEZONDAG\",\"match\":\"should\"},{\"value\":\"AUTO_GROOTNIEUWS\",\"match\":\"should\"},{\"value\":\"AUTO_ANDERMANSVEREN\",\"match\":\"should\"},{\"value\":\"AUTO_DESANDWICH\",\"match\":\"should\"},{\"value\":\"AUTO_GOUDMIJN\",\"match\":\"should\"},{\"value\":\"AUTO_ADRESONBEKEND\",\"match\":\"should\"},{\"value\":\"AUTO_LANGSHETTUINPADVANMIJNVADER\",\"match\":\"should\"},{\"value\":\"AUTO_DELAGELANDENLIJST\",\"match\":\"should\"},{\"value\":\"AUTO_EVERGREENTOPTOEGIFT\",\"match\":\"should\"},{\"value\":\"AUTO_THEATERVANHETSENTIMENT\",\"match\":\"should\"},{\"value\":\"AUTO_VETERANENDAG\",\"match\":\"should\"},{\"value\":\"AUTO_ZININWEEKEND\",\"match\":\"should\"},{\"value\":\"AUTO_ZINZOUTENZEGEN\",\"match\":\"should\"},{\"value\":\"AUTO_DEMUZIKALEFRUITMAND\",\"match\":\"should\"},{\"value\":\"AUTO_DEVERMOEDENVIERING\",\"match\":\"should\"},{\"value\":\"AUTO_MUSICARELIGIOSA\",\"match\":\"should\"},{\"value\":\"AUTO_NPORADIOMUZIEKNACHT\",\"match\":\"should\"},{\"value\":\"T_POW_03427508\",\"match\":\"should\"},{\"value\":\"T_RKK_1590045\",\"match\":\"should\"},{\"value\":\"T_POW_03420925\",\"match\":\"should\"},{\"value\":\"T_POW_00632198\",\"match\":\"should\"},{\"value\":\"T_POW_00597534\",\"match\":\"should\"},{\"value\":\"T_POW_03427343\",\"match\":\"should\"},{\"value\":\"T_POW_03424439\",\"match\":\"should\"},{\"value\":\"T_VPWON_1221291\",\"match\":\"should\"},{\"value\":\"T_POW_03430151\",\"match\":\"should\"},{\"value\":\"T_POW_03424435\",\"match\":\"should\"},{\"value\":\"T_POW_03424436\",\"match\":\"should\"},{\"value\":\"T_POW_03430175\",\"match\":\"should\"},{\"value\":\"T_POW_00597616\",\"match\":\"should\"},{\"value\":\"T_POW_00597614\",\"match\":\"should\"},{\"value\":\"T_POW_03430176\",\"match\":\"should\"},{\"value\":\"VPWON_1270183\",\"match\":\"should\"},{\"value\":\"POW_00803099\",\"match\":\"should\"},{\"value\":\"VARA_101380528\",\"match\":\"should\"},{\"value\":\"VPWON_1263203\",\"match\":\"should\"},{\"value\":\"VPWON_1268441\",\"match\":\"should\"},{\"value\":\"AT_2066037\",\"match\":\"should\"},{\"value\":\"VPWON_1265253\",\"match\":\"should\"},{\"value\":\"POW_02989177\",\"match\":\"should\"},{\"value\":\"VARA_101381388\",\"match\":\"should\"},{\"value\":\"VARA_101380908\",\"match\":\"should\"},{\"value\":\"VARA_101381402\",\"match\":\"should\"},{\"value\":\"POW_02993277\",\"match\":\"should\"},{\"value\":\"POW_03344542\",\"match\":\"should\"},{\"value\":\"POW_02990235\",\"match\":\"should\"},{\"value\":\"KN_1686655\",\"match\":\"should\"},{\"value\":\"KN_1685633\",\"match\":\"should\"},{\"value\":\"POW_03344621\",\"match\":\"should\"},{\"value\":\"VPWON_1264864\",\"match\":\"should\"},{\"value\":\"VPWON_1263422\",\"match\":\"should\"},{\"value\":\"VPWON_1263248\",\"match\":\"should\"},{\"value\":\"VPWON_1248677\",\"match\":\"should\"},{\"value\":\"POW_01003071\",\"match\":\"should\"},{\"value\":\"VPWON_1258754\",\"match\":\"should\"},{\"value\":\"VPWON_1270180\",\"match\":\"should\"},{\"value\":\"VPWON_1266069\",\"match\":\"should\"},{\"value\":\"POW_00803096\",\"match\":\"should\"},{\"value\":\"POMS_S_VPRO_171668\",\"match\":\"should\"},{\"value\":\"POMS_S_VPRO_170116\",\"match\":\"should\"},{\"value\":\"POMS_S_MAX_544172\",\"match\":\"should\"},{\"value\":\"POW_00811764\",\"match\":\"should\"},{\"value\":\"VARA_101380525\",\"match\":\"should\"},{\"value\":\"VARA_101377820\",\"match\":\"should\"},{\"value\":\"VPWON_1251880\",\"match\":\"should\"},{\"value\":\"VPWON_1246713\",\"match\":\"should\"},{\"value\":\"AT_2056745\",\"match\":\"should\"},{\"value\":\"AT_2056744\",\"match\":\"should\"},{\"value\":\"VPWON_1258837\",\"match\":\"should\"},{\"value\":\"VPWON_1258838\",\"match\":\"should\"},{\"value\":\"16Jnl1300n2za\",\"match\":\"should\"},{\"value\":\"NOSjnl1300\",\"match\":\"should\"},{\"value\":\"VARA_101381383\",\"match\":\"should\"},{\"value\":\"VARA_101377917\",\"match\":\"should\"},{\"value\":\"VARA_101380906\",\"match\":\"should\"},{\"value\":\"VARA_101380905\",\"match\":\"should\"},{\"value\":\"VARA_101381397\",\"match\":\"should\"},{\"value\":\"VARA_101381396\",\"match\":\"should\"},{\"value\":\"16ROFza\",\"match\":\"should\"},{\"value\":\"FryslanDOK\",\"match\":\"should\"},{\"value\":\"POW_03344527\",\"match\":\"should\"},{\"value\":\"POW_03078238\",\"match\":\"should\"},{\"value\":\"16Jnl1700\",\"match\":\"should\"},{\"value\":\"NOSjnl1700\",\"match\":\"should\"},{\"value\":\"KN_1676731\",\"match\":\"should\"},{\"value\":\"KN_1676732\",\"match\":\"should\"},{\"value\":\"KN_1676720\",\"match\":\"should\"},{\"value\":\"KN_1676721\",\"match\":\"should\"},{\"value\":\"POMS_S_NPO_1398276\",\"match\":\"should\"},{\"value\":\"POMS_S_NPO_1435647\",\"match\":\"should\"},{\"value\":\"POW_03344602\",\"match\":\"should\"},{\"value\":\"POW_03108618\",\"match\":\"should\"},{\"value\":\"VPWON_1245964\",\"match\":\"should\"},{\"value\":\"VPWON_1257897\",\"match\":\"should\"},{\"value\":\"VPWON_1256279\",\"match\":\"should\"},{\"value\":\"VPWON_1257874\",\"match\":\"should\"},{\"value\":\"VPWON_1248334\",\"match\":\"should\"},{\"value\":\"VPWON_1246712\",\"match\":\"should\"},{\"value\":\"VPWON_1258663\",\"match\":\"should\"},{\"value\":\"VPWON_1257894\",\"match\":\"should\"},{\"value\":\"POW_03027761\",\"match\":\"should\"},{\"value\":\"POW_02985259\",\"match\":\"should\"},{\"value\":\"POW_02985519\",\"match\":\"should\"},{\"value\":\"POW_03259207\",\"match\":\"should\"},{\"value\":\"POW_02985912\",\"match\":\"should\"},{\"value\":\"POW_03259448\",\"match\":\"should\"},{\"value\":\"POW_02986479\",\"match\":\"should\"},{\"value\":\"POW_03259547\",\"match\":\"should\"},{\"value\":\"POW_03355327\",\"match\":\"should\"},{\"value\":\"POW_03259638\",\"match\":\"should\"},{\"value\":\"POW_02987440\",\"match\":\"should\"},{\"value\":\"POW_03352469\",\"match\":\"should\"},{\"value\":\"POW_03028283\",\"match\":\"should\"},{\"value\":\"POW_03027762\",\"match\":\"should\"},{\"value\":\"POW_03355361\",\"match\":\"should\"},{\"value\":\"POW_03352470\",\"match\":\"should\"},{\"value\":\"POW_03221442\",\"match\":\"should\"},{\"value\":\"POW_03259288\",\"match\":\"should\"},{\"value\":\"POW_03348690\",\"match\":\"should\"},{\"value\":\"POW_03352472\",\"match\":\"should\"},{\"value\":\"POW_02988800\",\"match\":\"should\"},{\"value\":\"POW_03352473\",\"match\":\"should\"},{\"value\":\"POW_02989388\",\"match\":\"should\"},{\"value\":\"POW_03352474\",\"match\":\"should\"},{\"value\":\"POW_02989662\",\"match\":\"should\"},{\"value\":\"POW_03352475\",\"match\":\"should\"},{\"value\":\"POW_03352476\",\"match\":\"should\"},{\"value\":\"AT_2069404\",\"match\":\"should\"},{\"value\":\"POW_03374604\",\"match\":\"should\"},{\"value\":\"POW_03259289\",\"match\":\"should\"},{\"value\":\"POW_03352477\",\"match\":\"should\"},{\"value\":\"POW_02990632\",\"match\":\"should\"},{\"value\":\"AT_2061550\",\"match\":\"should\"},{\"value\":\"POW_03352284\",\"match\":\"should\"},{\"value\":\"KN_1686927\",\"match\":\"should\"},{\"value\":\"POW_02990957\",\"match\":\"should\"},{\"value\":\"KN_1686055\",\"match\":\"should\"},{\"value\":\"VPWON_1241869\",\"match\":\"should\"},{\"value\":\"POW_03385146\",\"match\":\"should\"},{\"value\":\"VPWON_1267023\",\"match\":\"should\"},{\"value\":\"POW_02992150\",\"match\":\"should\"},{\"value\":\"POW_03390240\",\"match\":\"should\"},{\"value\":\"POW_00976049\",\"match\":\"should\"},{\"value\":\"POW_03027507\",\"match\":\"should\"},{\"value\":\"POMS_S_NPO_172388\",\"match\":\"should\"},{\"value\":\"POW_03108298\",\"match\":\"should\"},{\"value\":\"16Jnl0630n1\",\"match\":\"should\"},{\"value\":\"NOSjnl0630\",\"match\":\"should\"},{\"value\":\"16Jnl0700n1\",\"match\":\"should\"},{\"value\":\"NOSjnl0700\",\"match\":\"should\"},{\"value\":\"POW_03259118\",\"match\":\"should\"},{\"value\":\"POW_03333061\",\"match\":\"should\"},{\"value\":\"16Jnl0730n1\",\"match\":\"should\"},{\"value\":\"NOSjnl0730\",\"match\":\"should\"},{\"value\":\"POW_03259359\",\"match\":\"should\"},{\"value\":\"16Jnl0800n1\",\"match\":\"should\"},{\"value\":\"NOSjnl0800\",\"match\":\"should\"},{\"value\":\"POW_03259458\",\"match\":\"should\"},{\"value\":\"16Jnl0830n1\",\"match\":\"should\"},{\"value\":\"NOSjnl0830\",\"match\":\"should\"},{\"value\":\"POW_03259549\",\"match\":\"should\"},{\"value\":\"16Jnl0900n1\",\"match\":\"should\"},{\"value\":\"NOSjnl0900\",\"match\":\"should\"},{\"value\":\"16actSeriousRequest\",\"match\":\"should\"},{\"value\":\"POW_03028028\",\"match\":\"should\"},{\"value\":\"POW_03108318\",\"match\":\"should\"},{\"value\":\"16Jnl1000\",\"match\":\"should\"},{\"value\":\"NOSjnl1000\",\"match\":\"should\"},{\"value\":\"16Jnl1100\",\"match\":\"should\"},{\"value\":\"NOSjnl1100\",\"match\":\"should\"},{\"value\":\"POW_03259210\",\"match\":\"should\"},{\"value\":\"POW_03108338\",\"match\":\"should\"},{\"value\":\"16Jnl1200n1\",\"match\":\"should\"},{\"value\":\"NOSjnl1200\",\"match\":\"should\"},{\"value\":\"16Jnl1300n1\",\"match\":\"should\"},{\"value\":\"AUTO_SPORTJOURNAAL\",\"match\":\"should\"},{\"value\":\"16Jnl1400\",\"match\":\"should\"},{\"value\":\"NOSjnl1400\",\"match\":\"should\"},{\"value\":\"16Jnl1500\",\"match\":\"should\"},{\"value\":\"NOSjnl1500\",\"match\":\"should\"},{\"value\":\"AT_2069413\",\"match\":\"should\"},{\"value\":\"AT_2069412\",\"match\":\"should\"},{\"value\":\"16Jnl1700n1\",\"match\":\"should\"},{\"value\":\"16Jnl1800\",\"match\":\"should\"},{\"value\":\"NOSjnl1800\",\"match\":\"should\"},{\"value\":\"AT_2046032\",\"match\":\"should\"},{\"value\":\"AT_2046031\",\"match\":\"should\"},{\"value\":\"16stspjnl1845\",\"match\":\"should\"},{\"value\":\"NOSSportjournaal\",\"match\":\"should\"},{\"value\":\"KN_1686695\",\"match\":\"should\"},{\"value\":\"KN_1686696\",\"match\":\"should\"},{\"value\":\"16Jnl2000mavr\",\"match\":\"should\"},{\"value\":\"NOSjnl2000\",\"match\":\"should\"},{\"value\":\"KN_1685277\",\"match\":\"should\"},{\"value\":\"KN_1685278\",\"match\":\"should\"},{\"value\":\"VPWON_1241863\",\"match\":\"should\"},{\"value\":\"VPWON_1261609\",\"match\":\"should\"},{\"value\":\"POW_03385144\",\"match\":\"should\"},{\"value\":\"POW_03385148\",\"match\":\"should\"},{\"value\":\"VPWON_1267021\",\"match\":\"should\"},{\"value\":\"VPWON_1262918\",\"match\":\"should\"},{\"value\":\"16Jnllaatmavr\",\"match\":\"should\"},{\"value\":\"NOSjnlLaat\",\"match\":\"should\"},{\"value\":\"POW_03390236\",\"match\":\"should\"},{\"value\":\"POW_03108419\",\"match\":\"should\"},{\"value\":\"POW_00975561\",\"match\":\"should\"},{\"value\":\"POW_00792469\",\"match\":\"should\"},{\"value\":\"POW_00796198\",\"match\":\"should\"},{\"value\":\"POW_00796201\",\"match\":\"should\"},{\"value\":\"POW_02951888\",\"match\":\"should\"},{\"value\":\"POW_00796218\",\"match\":\"should\"},{\"value\":\"POW_03071118\",\"match\":\"should\"},{\"value\":\"POW_00796236\",\"match\":\"should\"},{\"value\":\"POW_00796234\",\"match\":\"should\"},{\"value\":\"POW_00797216\",\"match\":\"should\"},{\"value\":\"POW_00796239\",\"match\":\"should\"},{\"value\":\"POW_03071098\",\"match\":\"should\"},{\"value\":\"POW_00793660\",\"match\":\"should\"},{\"value\":\"POW_00796240\",\"match\":\"should\"},{\"value\":\"POW_01127471\",\"match\":\"should\"},{\"value\":\"AUTO_RADIOSPORTZOMER\",\"match\":\"should\"},{\"value\":\"AUTO_WOORD\",\"match\":\"should\"},{\"value\":\"AUTO_NOSRADIOJOURNAAL\",\"match\":\"should\"},{\"value\":\"AUTO_RADIOJOURNAAL\",\"match\":\"should\"},{\"value\":\"AUTO_DEOCHTEND\",\"match\":\"should\"},{\"value\":\"AUTO_HETGELUIDVAN\",\"match\":\"should\"},{\"value\":\"AUTO_KONINGSDAGOP\",\"match\":\"should\"},{\"value\":\"AUTO_DENIEUWSBV\",\"match\":\"should\"},{\"value\":\"AUTO_PRINSJESDAG\",\"match\":\"should\"},{\"value\":\"AUTO_RADIOGIRODITALIA\",\"match\":\"should\"},{\"value\":\"AUTO_RADIOEENVANDAAG\",\"match\":\"should\"},{\"value\":\"AUTO_RADIOTOURDEFRANCE\",\"match\":\"should\"},{\"value\":\"AUTO_NIEUWSENCO\",\"match\":\"should\"},{\"value\":\"AUTO_DITISDEDAG\",\"match\":\"should\"},{\"value\":\"AUTO_NOSDODENHERDENKING\",\"match\":\"should\"},{\"value\":\"AUTO_RADIOGRANDDPART\",\"match\":\"should\"},{\"value\":\"AUTO_KUNSTSTOF\",\"match\":\"should\"},{\"value\":\"AUTO_MANGIARE\",\"match\":\"should\"},{\"value\":\"AUTO_NOSLANGSDELIJN\",\"match\":\"should\"},{\"value\":\"AUTO_BUREAUBUITENLAND\",\"match\":\"should\"},{\"value\":\"AUTO_DEHAAGSELOBBY\",\"match\":\"should\"},{\"value\":\"AUTO_LANGSDELIJNENOMSTREKEN\",\"match\":\"should\"},{\"value\":\"AUTO_NOSNEDERLANDKIEST\",\"match\":\"should\"},{\"value\":\"AUTO_METHETOOGOPMORGEN\",\"match\":\"should\"},{\"value\":\"AUTO_NOSMETHETOOGOPMORGEN\",\"match\":\"should\"},{\"value\":\"AUTO_BRAINWASHZOMERRADIO\",\"match\":\"should\"},{\"value\":\"AUTO_GROTEWOORDEN\",\"match\":\"should\"},{\"value\":\"AUTO_NOOITMEERSLAPEN\",\"match\":\"should\"},{\"value\":\"AUTO_DITISDENACHT\",\"match\":\"should\"},{\"value\":\"T_VPWON_1250370\",\"match\":\"should\"},{\"value\":\"T_POW_03427465\",\"match\":\"should\"},{\"value\":\"T_POW_03427519\",\"match\":\"should\"},{\"value\":\"T_POW_03430150\",\"match\":\"should\"},{\"value\":\"T_POW_03430154\",\"match\":\"should\"},{\"value\":\"T_POW_03430155\",\"match\":\"should\"},{\"value\":\"T_POW_03430152\",\"match\":\"should\"},{\"value\":\"T_POW_03430153\",\"match\":\"should\"},{\"value\":\"T_POW_03427345\",\"match\":\"should\"},{\"value\":\"T_POW_03424434\",\"match\":\"should\"},{\"value\":\"T_POW_03428761\",\"match\":\"should\"},{\"value\":\"T_POW_03430178\",\"match\":\"should\"},{\"value\":\"T_POW_03430157\",\"match\":\"should\"},{\"value\":\"T_POW_03430162\",\"match\":\"should\"},{\"value\":\"T_POW_03430159\",\"match\":\"should\"},{\"value\":\"T_POW_03430174\",\"match\":\"should\"},{\"value\":\"T_POW_03310742\",\"match\":\"should\"},{\"value\":\"T_POW_00133105\",\"match\":\"should\"},{\"value\":\"POW_00792470\",\"match\":\"should\"},{\"value\":\"POW_00793619\",\"match\":\"should\"},{\"value\":\"POW_00793621\",\"match\":\"should\"},{\"value\":\"POW_00792483\",\"match\":\"should\"},{\"value\":\"POW_00793623\",\"match\":\"should\"},{\"value\":\"POW_00793636\",\"match\":\"should\"},{\"value\":\"POW_00793637\",\"match\":\"should\"},{\"value\":\"POW_03140158\",\"match\":\"should\"},{\"value\":\"POW_00824900\",\"match\":\"should\"},{\"value\":\"POW_00889120\",\"match\":\"should\"},{\"value\":\"POW_00793663\",\"match\":\"should\"},{\"value\":\"POW_01023580\",\"match\":\"should\"},{\"value\":\"POW_00793691\",\"match\":\"should\"},{\"value\":\"AUTO_NIEUWSSHOW\",\"match\":\"should\"},{\"value\":\"AUTO_DETAALSTAAT\",\"match\":\"should\"},{\"value\":\"AUTO_LANGSDELIJN\",\"match\":\"should\"},{\"value\":\"AUTO_NOSJOURNAAL\",\"match\":\"should\"},{\"value\":\"AUTO_KAMERBREED\",\"match\":\"should\"},{\"value\":\"AUTO_ARGOS\",\"match\":\"should\"},{\"value\":\"AUTO_WNLOPZATERDAG\",\"match\":\"should\"},{\"value\":\"AUTO_WNLOPINIEMAKERS\",\"match\":\"should\"},{\"value\":\"AUTO_DEOVERNACHTING\",\"match\":\"should\"},{\"value\":\"AUTO_DEWERELDVANBNN\",\"match\":\"should\"},{\"value\":\"AUTO_DEDRUKTEMAKERS\",\"match\":\"should\"},{\"value\":\"AUTO_START\",\"match\":\"should\"},{\"value\":\"POW_00797637\",\"match\":\"should\"},{\"value\":\"POW_00797645\",\"match\":\"should\"},{\"value\":\"POW_00797646\",\"match\":\"should\"},{\"value\":\"POW_00797648\",\"match\":\"should\"},{\"value\":\"POW_03079258\",\"match\":\"should\"},{\"value\":\"POW_03079259\",\"match\":\"should\"},{\"value\":\"POW_03079278\",\"match\":\"should\"},{\"value\":\"POW_03079288\",\"match\":\"should\"},{\"value\":\"POW_00797657\",\"match\":\"should\"},{\"value\":\"AUTO_JUKEBOXTOP\",\"match\":\"should\"},{\"value\":\"AUTO_NPORADIOEVERGREENTOP\",\"match\":\"should\"},{\"value\":\"AUTO_TOPVANDEJAREN\",\"match\":\"should\"},{\"value\":\"AUTO_WEKKERWAKKER\",\"match\":\"should\"},{\"value\":\"AUTO_ARBEIDSVITAMINEN\",\"match\":\"should\"},{\"value\":\"AUTO_DETOPVANDEJAREN\",\"match\":\"should\"},{\"value\":\"AUTO_EVERGREENTOP\",\"match\":\"should\"},{\"value\":\"AUTO_EVERGREENTOPSTEMFINALEDAG\",\"match\":\"should\"},{\"value\":\"AUTO_TINEKESHOW\",\"match\":\"should\"},{\"value\":\"AUTO_OPENHUIS\",\"match\":\"should\"},{\"value\":\"AUTO_GROOTOP\",\"match\":\"should\"},{\"value\":\"AUTO_TIJDVOORMAXRADIO\",\"match\":\"should\"},{\"value\":\"AUTO_VOLGSPOT\",\"match\":\"should\"},{\"value\":\"AUTO_THUISOP\",\"match\":\"should\"},{\"value\":\"AUTO_EOMUZIEKNACHTNOSTALGIA\",\"match\":\"should\"},{\"value\":\"AUTO_MUZIEKNACHTNOSTALGIA\",\"match\":\"should\"},{\"value\":\"POW_00163249\",\"match\":\"should\"},{\"value\":\"POW_00995202\",\"match\":\"should\"},{\"value\":\"AT_2069821\",\"match\":\"should\"},{\"value\":\"VPWON_1258580\",\"match\":\"should\"},{\"value\":\"POW_03317967\",\"match\":\"should\"},{\"value\":\"VPWON_1226920\",\"match\":\"should\"},{\"value\":\"POW_03011720\",\"match\":\"should\"},{\"value\":\"KN_1685317\",\"match\":\"should\"},{\"value\":\"AT_2067586\",\"match\":\"should\"},{\"value\":\"POW_00240538\",\"match\":\"should\"},{\"value\":\"POW_00892977\",\"match\":\"should\"},{\"value\":\"POW_01046865\",\"match\":\"should\"},{\"value\":\"POW_00815902\",\"match\":\"should\"},{\"value\":\"POW_00297248\",\"match\":\"should\"},{\"value\":\"POW_00657247\",\"match\":\"should\"},{\"value\":\"POW_02992377\",\"match\":\"should\"},{\"value\":\"VPWON_1261180\",\"match\":\"should\"},{\"value\":\"POW_00448376\",\"match\":\"should\"},{\"value\":\"POW_00803957\",\"match\":\"should\"},{\"value\":\"KN_1678832\",\"match\":\"should\"},{\"value\":\"POW_02934189\",\"match\":\"should\"},{\"value\":\"AT_2067585\",\"match\":\"should\"},{\"value\":\"POW_03068061\",\"match\":\"should\"},{\"value\":\"POW_03289027\",\"match\":\"should\"},{\"value\":\"POW_02197064\",\"match\":\"should\"},{\"value\":\"POW_00201255\",\"match\":\"should\"},{\"value\":\"POW_00435140\",\"match\":\"should\"},{\"value\":\"VPWON_1258453\",\"match\":\"should\"},{\"value\":\"POW_00995203\",\"match\":\"should\"},{\"value\":\"POW_00441310\",\"match\":\"should\"},{\"value\":\"POW_03108358\",\"match\":\"should\"},{\"value\":\"POW_00304313\",\"match\":\"should\"},{\"value\":\"POW_00747963\",\"match\":\"should\"},{\"value\":\"POW_00457159\",\"match\":\"should\"},{\"value\":\"POW_00791118\",\"match\":\"should\"},{\"value\":\"KN_1685312\",\"match\":\"should\"},{\"value\":\"POW_00370639\",\"match\":\"should\"},{\"value\":\"POW_00365662\",\"match\":\"should\"},{\"value\":\"POW_00319350\",\"match\":\"should\"},{\"value\":\"VPWON_1264283\",\"match\":\"should\"},{\"value\":\"AT_2066205\",\"match\":\"should\"},{\"value\":\"KN_1685199\",\"match\":\"should\"},{\"value\":\"VPWON_1267239\",\"match\":\"should\"},{\"value\":\"POW_00567767\",\"match\":\"should\"},{\"value\":\"VPWON_1268422\",\"match\":\"should\"},{\"value\":\"AT_2065201\",\"match\":\"should\"},{\"value\":\"AT_2064261\",\"match\":\"should\"},{\"value\":\"VPWON_1239641\",\"match\":\"should\"},{\"value\":\"POW_02992964\",\"match\":\"should\"},{\"value\":\"KN_1686603\",\"match\":\"should\"},{\"value\":\"BNN_101381518\",\"match\":\"should\"},{\"value\":\"AT_2064551\",\"match\":\"should\"},{\"value\":\"AT_2072043\",\"match\":\"should\"},{\"value\":\"POW_03407247\",\"match\":\"should\"},{\"value\":\"POW_03175683\",\"match\":\"should\"},{\"value\":\"VARA_101381529\",\"match\":\"should\"},{\"value\":\"POW_03388868\",\"match\":\"should\"},{\"value\":\"POW_00809193\",\"match\":\"should\"},{\"value\":\"POW_00163215\",\"match\":\"should\"},{\"value\":\"POW_00818800\",\"match\":\"should\"},{\"value\":\"POMS_S_VPRO_113340\",\"match\":\"should\"},{\"value\":\"POW_00995187\",\"match\":\"should\"},{\"value\":\"POMS_S_VPRO_787855\",\"match\":\"should\"},{\"value\":\"POW_00995244\",\"match\":\"should\"},{\"value\":\"AT_2028850\",\"match\":\"should\"},{\"value\":\"POMS_S_TROS_100062\",\"match\":\"should\"},{\"value\":\"AT_2048906\",\"match\":\"should\"},{\"value\":\"VPWON_1258496\",\"match\":\"should\"},{\"value\":\"VPWON_1247335\",\"match\":\"should\"},{\"value\":\"POW_03317942\",\"match\":\"should\"},{\"value\":\"POW_03382279\",\"match\":\"should\"},{\"value\":\"VPWON_1226917\",\"match\":\"should\"},{\"value\":\"VPWON_1267028\",\"match\":\"should\"},{\"value\":\"POW_03011707\",\"match\":\"should\"},{\"value\":\"POW_03158518\",\"match\":\"should\"},{\"value\":\"KN_1680324\",\"match\":\"should\"},{\"value\":\"KN_1680323\",\"match\":\"should\"},{\"value\":\"AT_2066756\",\"match\":\"should\"},{\"value\":\"AT_2066755\",\"match\":\"should\"},{\"value\":\"POW_00240526\",\"match\":\"should\"},{\"value\":\"POMS_S_VPRO_217078\",\"match\":\"should\"},{\"value\":\"POMS_S_VPRO_084303\",\"match\":\"should\"},{\"value\":\"POW_00816641\",\"match\":\"should\"},{\"value\":\"POW_00892961\",\"match\":\"should\"},{\"value\":\"POMS_S_KRO_714185\",\"match\":\"should\"},{\"value\":\"POW_00893020\",\"match\":\"should\"},{\"value\":\"POW_01046861\",\"match\":\"should\"},{\"value\":\"POW_01047543\",\"match\":\"should\"},{\"value\":\"POW_00815880\",\"match\":\"should\"},{\"value\":\"POMS_S_VARA_578599\",\"match\":\"should\"},{\"value\":\"POW_00822800\",\"match\":\"should\"},{\"value\":\"POW_00297225\",\"match\":\"should\"},{\"value\":\"POMS_S_EO_516009\",\"match\":\"should\"},{\"value\":\"POW_00887160\",\"match\":\"should\"},{\"value\":\"POW_00657245\",\"match\":\"should\"},{\"value\":\"POMS_S_VPRO_465305\",\"match\":\"should\"},{\"value\":\"POW_00890542\",\"match\":\"should\"},{\"value\":\"16Jeugd0845\",\"match\":\"should\"},{\"value\":\"NOSjeugd0845\",\"match\":\"should\"},{\"value\":\"VPWON_1261168\",\"match\":\"should\"},{\"value\":\"VPWON_1251200\",\"match\":\"should\"},{\"value\":\"POW_00448368\",\"match\":\"should\"},{\"value\":\"POW_00812396\",\"match\":\"should\"},{\"value\":\"POMS_S_AVRO_120019\",\"match\":\"should\"},{\"value\":\"POW_00803916\",\"match\":\"should\"},{\"value\":\"POMS_S_TROS_123299\",\"match\":\"should\"},{\"value\":\"POW_00892940\",\"match\":\"should\"},{\"value\":\"KN_1676501\",\"match\":\"should\"},{\"value\":\"KN_1674156\",\"match\":\"should\"},{\"value\":\"POW_02934132\",\"match\":\"should\"},{\"value\":\"POW_02934232\",\"match\":\"should\"},{\"value\":\"POW_03068012\",\"match\":\"should\"},{\"value\":\"POW_03068578\",\"match\":\"should\"},{\"value\":\"POW_03288981\",\"match\":\"should\"},{\"value\":\"POW_03269518\",\"match\":\"should\"},{\"value\":\"POW_02196985\",\"match\":\"should\"}]}},\"highlight\":false}";

        PageForm form = Jackson2Mapper.getLenientInstance().readerFor(PageForm.class).readValue(new StringReader(largeForm));

        PageSearchResult resultItems = clientsShortTimeouts.getPageService().find(form, "vpro-predictions", null, 0L, 240);

        System.out.println(resultItems);
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
