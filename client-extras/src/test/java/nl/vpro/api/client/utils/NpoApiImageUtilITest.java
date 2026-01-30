package nl.vpro.api.client.utils;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

import nl.vpro.util.ConfigUtils;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 */
@Slf4j
class NpoApiImageUtilITest {

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void getGetSize() {

        NpoApiImageUtil util = new NpoApiImageUtil("https://images-test.poms.omroep.nl/");
        ConfigUtils.configuredInHome(util, "apiclient.properties");
        log.info("Testing for {}", util.getBaseUrl());
        assertThat(util.getSize("urn:vpro:image:706133").getAsLong()).isEqualTo(213356L);

        assertThat(util.isAvailable()).isTrue();
    }
}
