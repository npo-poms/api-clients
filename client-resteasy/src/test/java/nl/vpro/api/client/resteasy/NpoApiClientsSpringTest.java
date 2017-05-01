package nl.vpro.api.client.resteasy;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Java6Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 5.3
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:client-resteasy-test-context.xml")
public class NpoApiClientsSpringTest {

    @Inject
    NpoApiClients clients;

    @Test
    public void test() {
        assertThat(clients.getOrigin()).isEqualTo("http://www.vpro.nl");
    }
}
