/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.MediaFormBuilder;
import nl.vpro.domain.api.media.MediaSearchResult;
import nl.vpro.jackson2.Jackson2Mapper;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 4.3
 */
@Ignore
public class NpoApiClientsMediaITest {

    private static final Logger LOG = LoggerFactory.getLogger(NpoApiClientsMediaITest.class);

    private static NpoApiClients clients;

    @BeforeClass
    public static void setUp() throws IOException {
        clients = NpoApiClients.configured().build();
        System.out.println("Testing with " + clients);
    }

    @Test
    public void testDescendants() throws JsonProcessingException {
        MediaForm form =
            MediaFormBuilder.form().build();

        LOG.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

        MediaSearchResult result = clients.getMediaService().findDescendants(form, "POMS_S_NTR_3779230", null, null, 0L, 4);

        assertThat(result.asList()).hasSize(4);

    }


    @Test
    public void testFindInProfile() throws JsonProcessingException {
        MediaForm form =
            MediaFormBuilder.form().build();

        LOG.info(Jackson2Mapper.getLenientInstance().writeValueAsString(form));

        MediaSearchResult result = clients.getMediaService().find(form, "netinnl", null, 0L, 10);

        assertThat(result.asList()).hasSize(10);

    }
}
