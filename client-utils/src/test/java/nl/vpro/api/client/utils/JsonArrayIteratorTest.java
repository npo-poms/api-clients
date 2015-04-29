package nl.vpro.api.client.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;

import nl.vpro.domain.api.Change;

import static org.fest.assertions.Assertions.assertThat;

public class JsonArrayIteratorTest {


    @Test
    public void test() throws IOException {
        JsonArrayIterator<Change> it = new JsonArrayIterator<>(getClass().getResourceAsStream("/changes.json"), Change.class, null);
        assertThat(it.next().getMid()).isEqualTo("POMS_NCRV_1138990");
        for (int i = 0; i < 8; i++) {
            assertThat(it.hasNext()).isTrue();

            Change change = it.next();
            if (!change.isDeleted()) {
                assertThat(change.getMedia()).isNotNull();
            }
        }
        assertThat(it.hasNext()).isTrue();
        assertThat(it.next().getMid()).isEqualTo("POMS_VPRO_1139788");
        assertThat(it.hasNext()).isFalse();
    }


    @Test
    public void testEmpty() throws IOException {
        JsonArrayIterator<Change> it = new JsonArrayIterator<>(new ByteArrayInputStream("{\"array\":[]}".getBytes()), Change.class, null);
        assertThat(it.hasNext()).isFalse();
        assertThat(it.hasNext()).isFalse();

    }
}
