package nl.vpro.api.client.frontend;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 5.3
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:client-resteasy-test-context.xml")
public class NpoApiClientsSpringTest {

    @Inject
	NpoApiClients clients;

    @Test
    public void test() {
        assertThat(clients.getOrigin()).isEqualTo("https://www.vpro.nl");
    }
}
