package nl.vpro.api.client.utils;

import java.net.URL;
import java.time.Instant;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Ignore;
import org.junit.Test;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.domain.api.Change;
import nl.vpro.domain.api.Order;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@Ignore
public class MediaRestClientUtilsTest {

    @Test
    public void testChanges() throws Exception {
        
        MediaRestService mediaRestService = mock(MediaRestService.class);

        // michiel@baleno:~/github/npo-poms/api/bash/media$ ENV=prod ./changes.sh  1000000 vpro > /tmp/changes.json
        when(mediaRestService.changes(
            anyString(),
            anyString(),
            any(Long.class),
            any(Instant.class),
            eq("asc"),
            any(Integer.class),
            any(Boolean.class),
            any(HttpServletRequest.class),
            any(HttpServletResponse.class)))
            .thenReturn(new URL("file:////Users/michiel/npo/api-client/changes.json").openStream());
        Iterator<Change> i = MediaRestClientUtils.changes(mediaRestService, "vpro", Instant.ofEpochMilli(0), Order.ASC, Integer.MAX_VALUE);
        int count = 0;
        while(i.hasNext()) {
            Change next = i.next();
            if (! next.isDeleted()) {
                assertThat(next.getMedia()).isNotNull();
            }
            System.out.println(i.next());
            count++;
        }
        System.out.println(count);

    }

    @Test
    public void testLoad() {

    }

    @Test
    public void testToMid() {
        assertThat(MediaRestClientUtils.toMid("urn:vpro:media:program:1906")).isEqualTo("POMS_VPRO_158299");
    }
}
