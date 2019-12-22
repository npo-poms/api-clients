package nl.vpro.api.client.frontend;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.inject.*;
import com.google.inject.name.Names;

import nl.vpro.api.client.pages.PageUpdateApiClient;
import nl.vpro.api.client.utils.Config;
import nl.vpro.guice.Convertors;
import nl.vpro.guice.OptionalModule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 5.3
 */
@Slf4j
public class NpoApiClientsGuiceTest {


    private Injector injector;

    @BeforeEach
    public void setup() {
        injector = Guice.createInjector(
            new AbstractModule() {
                @Override
                protected void configure() {
                    Map<String, String> config = new HashMap<>();
                    config.putAll(new Config("apiclient-test.properties").getPrefixedProperties(Config.Prefix.npo_api));
                    config.putAll(new Config("apiclient-test.properties").getPrefixedProperties(Config.Prefix.npo_pageupdate_api));
                    Names.bindProperties(binder(), config);

                    binder().bind(NpoApiClients.class).toProvider(NpoApiClients.Provider.class);

                    binder().bind(PageUpdateApiClient.class).toProvider(PageUpdateApiClient.Provider.class);

                }

            },
            new Convertors(),
            new OptionalModule(NpoApiClients.Provider.class, PageUpdateApiClient.Provider.class)
        );


    }

    @Test
    public void test() {
        NpoApiClients clients  = injector.getInstance(NpoApiClients.class);
        assertThat(clients.getOrigin()).isEqualTo("https://www.vpro.nl");

        PageUpdateApiClient pageUpdateApiClient = injector.getInstance(PageUpdateApiClient.class);
        //pageUpdateApiClient.getPageUpdateRestService().save(new PageUpdate());
    }
}
