package nl.vpro.api.client.utils;

import java.net.URL;
import java.time.Instant;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.domain.api.*;
import nl.vpro.util.CloseableIterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@Disabled
public class MediaRestClientUtilsTest {

    @Test
    public void testChanges() throws Exception {

        MediaRestService mediaRestService = mock(MediaRestService.class);

        // michiel@baleno:~/github/npo-poms/api/bash/media$ ENV=prod ./changes.sh  1000000 vpro > /tmp/changes.json
        when(mediaRestService.changes(
                anyString(),
                anyString(),
                any(Long.class),
                anyString(),
                eq("asc"),
                any(Integer.class),
                any(Deletes.class),
                any(Tail.class),
            anyString()
            )
        ).thenReturn(Response.ok().entity(new URL("file:////Users/michiel/npo/api-client/changes.json").openStream()).build());
        int count = 0;
        try (CloseableIterator<MediaChange> i = MediaRestClientUtils.changes(mediaRestService, "vpro", Instant.ofEpochMilli(0), null, Order.ASC, Integer.MAX_VALUE, Deletes.ID_ONLY)) {

            while (i.hasNext()) {
                MediaChange next = i.next();
                if (!next.isDeleted()) {
                    assertThat(next.getMedia()).isNotNull();
                }
                System.out.println(i.next());
                count++;
            }
        }
        System.out.println(count);

    }

    @Test
    public void testLoad() {
    }

    @Test
    @Deprecated
    public void testToMid() {
        assertThat(MediaRestClientUtils.toMid("urn:vpro:media:program:1906")).isEqualTo("POMS_VPRO_158299");
    }

    @Test
    public void testLoadSubitltes() {

    }
}
