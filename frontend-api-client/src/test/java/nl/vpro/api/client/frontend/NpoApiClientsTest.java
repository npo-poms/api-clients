package nl.vpro.api.client.frontend;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

import nl.vpro.api.client.utils.Config;
import nl.vpro.util.Env;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Slf4j
public class NpoApiClientsTest {

    public static final Config CONFIG = new Config(
        "apiclient.properties",
        "apiclient-test3.properties"
        );


    @Test
    public void configured() {
        Env env = CONFIG.env();
        assertThat(env).isEqualTo(Env.TEST);
    /*    assertThat(CONFIG.getPrefixedProperties(Config.Prefix.npo_api).get("npo-api.baseUrl")).isEqualTo("https://rs-test.poms.omroep.nl/v1");
        assertThat(CONFIG.getProperties().get("npo-api.baseUrl.test")).isEqualTo("https://rs-test.poms.omroep.nl/v1");

        assertThat(CONFIG.getProperties(Config.Prefix.npo_api).get("baseUrl")).isEqualTo("https://rs-test.poms.omroep.nl/v1");


        log.info("{}", CONFIG.getProperties());*/
        NpoApiClients clients =
            NpoApiClients.configured(CONFIG.getProperties(Config.Prefix.npo_api))
                .build();

        assertThat(clients.getBaseUrl()).isEqualTo("https://rs-test.poms.omroep.nl/v1/api");

    }


    @Test
    public void configuredInHome() {

        NpoApiClients clients =
            NpoApiClients.configured(Env.TEST)
                .build();

        assertThat(clients.getBaseUrl()).isEqualTo("https://rs-test.poms.omroep.nl/v1/api");

    }

}
