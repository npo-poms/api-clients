package nl.vpro.api.client.frontend;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

import nl.vpro.api.client.utils.Config;
import nl.vpro.util.Env;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 */
@Slf4j
class NpoApiClientsTest {

    private static final Config CONFIG = new Config(
        "poms-urls.properties",
        "apiclient.properties",
        "apiclient-test3.properties"
        );


    @Test
    void configured() {
        Env env = CONFIG.env();
        assertThat(env).isEqualTo(Env.TEST);
        try (NpoApiClients clients =
            NpoApiClients.configured(CONFIG.getProperties(Config.Prefix.api))
                .build()) {

            assertThat(clients.getBaseUrl()).isEqualTo("https://rs-test.poms.omroep.nl/v1/api");
        }

    }


    @Test
    void configuredInHome() {

        try (NpoApiClients clients =
            NpoApiClients.configured(Env.TEST)
                .build()) {

            assertThat(clients.getBaseUrl()).isEqualTo("https://rs-test.poms.omroep.nl/v1/api");
        }

    }

}
