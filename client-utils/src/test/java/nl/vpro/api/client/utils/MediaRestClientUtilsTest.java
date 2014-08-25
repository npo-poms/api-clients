package nl.vpro.api.client.utils;

import java.net.URL;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.domain.api.Change;
import nl.vpro.domain.api.Order;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MediaRestClientUtilsTest {

    @Test
    public void testChanges() throws Exception {

        MediaRestClientUtils utils = new MediaRestClientUtils();

        MediaRestService mediaRestService = mock(MediaRestService.class);


        when(mediaRestService.changes(anyString(), anyString(), any(Long.class), eq("asc"), any(Integer.class), any(HttpServletRequest.class), any(HttpServletResponse.class))).thenReturn(new URL("file:///tmp/changes.json").openStream());
        Iterator<Change> i = utils.changes(mediaRestService, "human", 0, Order.ASC, Integer.MAX_VALUE);
        int count = 0;
        while(i.hasNext()) {
            System.out.println(i.next());
            count++;
        }
        System.out.println(count);

    }
}
