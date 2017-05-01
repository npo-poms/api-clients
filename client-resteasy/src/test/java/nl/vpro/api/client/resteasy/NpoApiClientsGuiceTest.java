package nl.vpro.api.client.resteasy;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

import nl.vpro.guice.Convertors;
import nl.vpro.guice.OptionalModule;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 5.3
 */
@Slf4j
public class NpoApiClientsGuiceTest {


    private Injector injector;

    @Before
    public void setup() {
        injector = Guice.createInjector(
            new AbstractModule() {
                @Override
                protected void configure() {
                    Properties properties = new Properties();
                    try {
                        InputStream inputStream = NpoApiClients.class.getClassLoader().getResourceAsStream("npoapiclients.properties");
                        System.out.println("" + inputStream);
                        properties.load(inputStream);
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                    Names.bindProperties(binder(), properties);
                    binder().bind(NpoApiClients.class).toProvider(NpoApiClients.Provider.class);
                }
            },
            new Convertors(),
            new OptionalModule(NpoApiClients.Provider.class)
        );


    }

    @Test
    public void test() {
        NpoApiClients clients  = injector.getInstance(NpoApiClients.class);
        assertThat(clients.getOrigin()).isEqualTo("http://www.vpro.nl");
    }
}
