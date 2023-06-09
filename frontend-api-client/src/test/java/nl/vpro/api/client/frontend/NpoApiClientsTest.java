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
public class NpoApiClientsTest {

    public static final Config CONFIG = new Config(
        "poms-urls.properties",
        "apiclient.properties",
        "apiclient-test3.properties"
        );


    @Test
    public void configured() {
        Env env = CONFIG.env();
        assertThat(env).isEqualTo(Env.TEST);
        NpoApiClients clients =
            NpoApiClients.configured(CONFIG.getProperties(Config.Prefix.api))
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
