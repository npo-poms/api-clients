package nl.vpro.api.client.utils;

import org.junit.Test;

import nl.vpro.util.Version;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 5.7
 */
public class SwaggerTest {

    @Test
    public void getVersion() {

        assertThat(Swagger.getVersion("5.6-SNAPSHOT.${builder.vcs.number}", null)).isEqualTo("5.6");

        assertThat(Swagger.getVersion("5.6.2.${builder.vcs.number}", null)).isEqualTo("5.6.2");

        assertThat(Swagger.getVersionNumber("5.6-SNAPSHOT.${builder.vcs.number}", null)).isEqualTo(Version.of(5, 6));

        assertThat(Swagger.getVersionNumber("5.6.2.${builder.vcs.number}", null)).isEqualTo(Version.of(5, 6, 2));



    }
}
