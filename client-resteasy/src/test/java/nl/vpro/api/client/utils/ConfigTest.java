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

    Config config = new Config("apiclient.properties");



    @Test
    public void env() throws Exception {


        Map<String, String> props = config.getProperties(Config.Prefix.npoapi);
        log.info("{}", props);


        assertThat(config.env()).isEqualTo(Env.TEST);
    }

    @Test
    public void setEnv() throws Exception {

        config.setEnv(Env.DEV);
        assertThat(config.env()).isEqualTo(Env.DEV);

        assertThat(config.requiredOption(Config.Prefix.npoapi, "baseUrl")).isEqualTo("https://rs-dev.poms.omroep.nl/v1");
        //assertThat(config.requiredOption(Config.Prefix.npoapi, "secret")).isEqualTo("bla");



    }

}
