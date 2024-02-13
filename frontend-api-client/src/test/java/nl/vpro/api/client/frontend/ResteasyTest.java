package nl.vpro.api.client.frontend;

import lombok.extern.slf4j.Slf4j;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.cache.BrowserCacheFeature;
import org.jboss.resteasy.client.jaxrs.cache.MapCache;
import org.junit.jupiter.api.Test;

import nl.vpro.api.client.resteasy.ResteasyHelper;

/**
 *
 * @author Michiel Meeuwissen
 */
@Slf4j
public class ResteasyTest {


    @Test
    public void testAcceptHeader() {
        ResteasyClientBuilder builder = ResteasyHelper.clientBuilder();
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
