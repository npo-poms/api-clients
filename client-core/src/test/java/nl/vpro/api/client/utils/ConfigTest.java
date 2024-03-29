package nl.vpro.api.client.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.junit.jupiter.api.Test;

import nl.vpro.util.Env;

import static nl.vpro.api.client.utils.Config.Prefix.api;
import static nl.vpro.api.client.utils.Config.Prefix.pages_publisher;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen

 */
@Slf4j
public class ConfigTest {


    @Test
    public void env() {
        Config config = new Config("apiclient-test.properties", "apiclient-test2.properties");
        Map<String, String> props = config.getProperties(api);
        log.info("{}", props);
        assertThat(config.env()).isEqualTo(Env.TEST);
        assertThat(props.get("baseUrl")).isEqualTo("https://rs-test.poms.omroep.nl/v1");
        assertThat(props.get("apiKey")).isEqualTo("KEY2");

    }

    @Test
    public void setEnv() {
        Config config = new Config("apiclient-test.properties", "apiclient-test2.properties");

        config.setEnv(Env.TEST);
        assertThat(config.env()).isEqualTo(Env.TEST);

        assertThat(config.requiredOption(api, "baseUrl")).isEqualTo("https://rs-test.poms.omroep.nl/v1");
        //assertThat(config.requiredOption(Config.Prefix.npoapi, "secret")).isEqualTo("bla");
        assertThat(config.getProperties(api).get("secret")).isEqualTo("testsecret2");
        assertThat(config.getProperties(api).get("apiKey")).isEqualTo("KEY2");

    }


    @Test
    public void envPerPrefix() {

        Config config = new Config("apiclient-test-env-prefix.properties");
        assertThat(config.env(api)).isEqualTo(Env.TEST);
        assertThat(config.env(pages_publisher)).isEqualTo(Env.LOCALHOST);
        {
            Map<String, String> props = config.getProperties(api);
            log.info("{}", props);
            assertThat(props.get("baseUrl")).isEqualTo("https://rs-test.poms.omroep.nl/v1");
        }
        {
            Map<String, String> props = config.getProperties(pages_publisher);
            log.info("{}", props);
            assertThat(props.get("baseUrl")).isEqualTo("https://localhost:8069/");
        }
    }

    @Test
    public void withSubst() {

        Config config = new Config("with-subst.properties");
        assertThat(config.requiredOption(api, "es.env")).isEqualTo("prod");

    }





}
