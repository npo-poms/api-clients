/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.frontend;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.domain.api.media.*;
import nl.vpro.domain.api.profile.exception.ProfileNotFoundException;
import nl.vpro.jackson2.Jackson2Mapper;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 4.3
 */
@Slf4j
public class NpoApiClientsMediaITest {

    private static NpoApiClients clients;

    @BeforeAll
    public static void setUp() {
        clients = NpoApiClients.configured().trustAll(true).build();
        System.out.println("Testing with " + clients);
    }

    @Test
    public void testDescendants() throws JsonProcessingException, ProfileNotFoundException {
        MediaForm form =
            MediaFormBuilder.form().build();

        log.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

        MediaSearchResult result = clients.getMediaService().findDescendants(form, "POMS_S_NTR_3779230", null, null, 0L, 4);

        assertThat(result.asList()).hasSize(4);

    }


    @Test
    public void testFindInProfile() throws JsonProcessingException, ProfileNotFoundException {
        MediaForm form =
            MediaFormBuilder.form().build();

        log.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

        MediaSearchResult result = clients.getMediaService().find(form, "netinnl", null, 0L, 10);

        assertThat(result.asList()).hasSize(10);

    }


    @Test
    public void testDescendantsProperties() throws JsonProcessingException, ProfileNotFoundException {
        MediaForm form =
            MediaFormBuilder.form().build();

        log.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));
        clients.setProperties("none");

        MediaSearchResult result = clients.getMediaService().findDescendants(form, "POMS_S_EO_179639", null, null, 0L, 4);

        assertThat(result.asList()).hasSize(4);

    }

}
