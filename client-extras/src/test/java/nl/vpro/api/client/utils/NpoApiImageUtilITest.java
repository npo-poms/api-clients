package nl.vpro.api.client.utils;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

import nl.vpro.util.ConfigUtils;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 */
@Slf4j
public class NpoApiImageUtilITest {

    @Test
    public void getGetSize() {

        NpoApiImageUtil util = new NpoApiImageUtil("https://images-dev.poms.omroep.nl/");
        ConfigUtils.configuredInHome(util, "apiclient.properties");
        log.info("Testing for {}", util.getBaseUrl());
        assertThat(util.getSize("urn:vpro:image:706133").get()).isEqualTo(213356L);
    }
}
