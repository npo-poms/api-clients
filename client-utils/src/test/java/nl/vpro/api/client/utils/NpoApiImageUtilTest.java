package nl.vpro.api.client.utils;

import org.junit.Test;

import nl.vpro.util.ConfigUtils;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public class NpoApiImageUtilTest {

    @Test
    public void getGetSize() {

        NpoApiImageUtil util = new NpoApiImageUtil("https://images.poms.omroep.nl/");

        ConfigUtils.configured(util, "apiclients.properties");

        assertThat(util.getSize("urn:image:123").get()).isEqualTo(1234l);
    }
}
