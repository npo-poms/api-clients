package nl.vpro.api.client.utils;

import java.net.MalformedURLException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.api.client.resteasy.PageUpdateApiClient;
import nl.vpro.domain.page.PageType;
import nl.vpro.domain.page.update.PageUpdate;

import static org.fest.assertions.Assertions.assertThat;

@Ignore("This required running server at publish-dev")
public class PageUpdateApiClientUtilTest  {

    private PageUpdateApiUtil util;

    //private String target = "http://publish-dev.poms.omroep.nl/";
    private String target = "http://localhost:8060/";

    @Before
    public void setUp() throws MalformedURLException {
        PageUpdateApiClient clients = new PageUpdateApiClient(
            target,
            "vpro-cms",
            "***REMOVED***", 1000);
        util = new PageUpdateApiUtil(clients, new PageUpdateRateLimiter());
    }

    @Test
    public void testSave() throws Exception {
        PageUpdate instance = new PageUpdate(PageType.ARTICLE, "http://vpro.nl/test");
        Result result = util.save(instance);
        assertThat(result.isSuccess()).isFalse();
        System.out.println(result.getErrors());
    }


    @Test
    public void testDelete() throws Exception {
        String id  = "http://BESTAATNIET";
        Result result = util.delete(id);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatus()).isEqualTo(Result.Status.NOTFOUND);

        System.out.println("errors " + result.getErrors());
    }
}
