package nl.vpro.api.client.utils;

import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.time.Instant;

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
class MediaRestClientUtilsTest {

    @Test
    void testChanges() throws Exception {

        MediaRestService mediaRestService = mock(MediaRestService.class);

        // michiel@baleno:~/github/npo-poms/api/bash/media$ ENV=prod ./changes.sh  1000000 vpro > /tmp/changes.json
        when(mediaRestService.changes(
            anyString(),
            anyString(),
            any(Long.class),
            anyString(),
            isNull(),
            any(Integer.class),
            any(Deletes.class),
            any(Tail.class),
            anyString()
            )
        ).thenReturn(Response.ok().entity(URI.create("file:////Users/michiel/npo/api-client/changes.json").toURL().openStream()).build());
        int count = 0;
        try (CloseableIterator<MediaChange> i = MediaRestClientUtils.changes(mediaRestService, "vpro", Instant.ofEpochMilli(0), null, null, Integer.MAX_VALUE, Deletes.ID_ONLY)) {

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
    void testLoad() {
    }


    @Test
    void testLoadSubitltes() {

    }
}
