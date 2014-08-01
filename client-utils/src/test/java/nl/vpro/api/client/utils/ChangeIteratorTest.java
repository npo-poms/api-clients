package nl.vpro.api.client.utils;

import java.io.IOException;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class ChangeIteratorTest {


    @Test
    public void test() throws IOException {
        ChangeIterator it = new ChangeIterator(getClass().getResourceAsStream("/changes.json"));
        assertThat(it.next().getMid()).isEqualTo("POMS_NCRV_1138990");
        for (int i = 0; i < 8; i++) {
            assertThat(it.hasNext()).isTrue();
            it.next();
        }
        assertThat(it.hasNext()).isTrue();
        assertThat(it.next().getMid()).isEqualTo("POMS_VPRO_1139788");
        assertThat(it.hasNext()).isFalse();
    }
}
