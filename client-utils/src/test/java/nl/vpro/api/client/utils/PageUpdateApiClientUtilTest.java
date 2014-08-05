package nl.vpro.api.client.utils;

import java.net.MalformedURLException;

import org.junit.Before;
import org.junit.Test;

import nl.vpro.api.client.resteasy.PageUpdateApiClient;
import nl.vpro.domain.page.PageType;
import nl.vpro.domain.page.update.PageUpdate;

import static org.fest.assertions.Assertions.assertThat;

public class PageUpdateApiClientUtilTest  {

    private PageUpdateApiClientUtil util;


    @Before
    public void setUp() throws MalformedURLException {
        PageUpdateApiClient clients = new PageUpdateApiClient(
            "http://publish-dev.poms.omroep.nl/",
            "vpro-cms",
            "***REMOVED***");
        util = new PageUpdateApiClientUtil(clients);
    }

    @Test
    public void testGetPageUpdateRestService() throws Exception {
        PageUpdate instance = new PageUpdate(PageType.NEWS, "http://vpro.nl/test");
        Result result = util.save(instance);
        assertThat(result.isSuccess()).isFalse();
        System.out.println(result.getErrors());
    }

}
