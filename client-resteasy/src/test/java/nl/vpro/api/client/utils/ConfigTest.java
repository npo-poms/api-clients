package nl.vpro.api.client.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.junit.Test;

import nl.vpro.util.Env;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Slf4j
public class ConfigTest {

    Config config = new Config("apiclient-test.properties", "apiclient-test2.properties");

    @Test
    public void env() throws Exception {
        Map<String, String> props = config.getProperties(Config.Prefix.npo_api);
        log.info("{}", props);
        assertThat(config.env()).isEqualTo(Env.TEST);
        assertThat(props.get("baseUrl")).isEqualTo("https://rs-test.poms.omroep.nl/v1");
    }


    @Test
    public void setEnv() throws Exception {

        config.setEnv(Env.DEV);
        assertThat(config.env()).isEqualTo(Env.DEV);

        assertThat(config.requiredOption(Config.Prefix.npo_api, "baseUrl")).isEqualTo("https://rs-dev.poms.omroep.nl/v1");
        //assertThat(config.requiredOption(Config.Prefix.npoapi, "secret")).isEqualTo("bla");
        assertThat(config.getProperties(Config.Prefix.npo_api).get("secret")).isEqualTo("devsecret2");


    }

}
