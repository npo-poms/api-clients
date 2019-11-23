package nl.vpro.api.client.frontend;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.cache.BrowserCacheFeature;
import org.jboss.resteasy.client.jaxrs.cache.MapCache;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.junit.jupiter.api.Test;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Slf4j
public class ResteasyTest {


    @Test
    public void testAcceptHeader() {
        ResteasyClientBuilder builder = new ResteasyClientBuilderImpl();
        BrowserCacheFeature browserCacheFeature = new BrowserCacheFeature();
        browserCacheFeature.setCache(new MapCache());
        builder.register(browserCacheFeature);

        TestInterface test = builder.build()
            .target("https://jsonplaceholder.typicode.com/posts/")
            .proxyBuilder(TestInterface.class)
            .build();
        String load = test.load("42");
        log.info("{}", load);

    }

    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    interface TestInterface {
          @GET
          @Path("/{id:.*}")
          String load(@Encoded @PathParam("id") String id);
    }
}
