package nl.vpro.api.client.frontend;

import java.net.URI;
import java.time.*;
import java.util.Map;

import org.junit.jupiter.api.Test;

import nl.vpro.poms.shared.Headers;

import static org.assertj.core.api.Assertions.assertThat;

class NpoApiAuthenticationTest {


    @Test
    public void test() {
        NpoApiAuthentication authentication = NpoApiAuthentication.builder()
            .origin("https://www.vpro.nl")
            .apiKey("foobar")
            .secret("secret")
            .clock(Clock.fixed(LocalDateTime.of(2021, 4, 29, 11, 10).atZone(ZoneId.of("Europe/Amsterdam")).toInstant(), ZoneId.of("Europe/Amsterdam")))
            .build();

        Map<String, Object> authenticate = authentication.authenticate(URI.create("https://rs.poms.omroep.nl/api/media?a=b"));
        assertThat(authenticate.get(Headers.NPO_DATE)).isEqualTo("Thu, 29 Apr 2021 09:10:00 GMT");
        assertThat(authenticate.get("Origin")).isEqualTo("https://www.vpro.nl");
        assertThat(authenticate.get("Authorization")).isEqualTo("NPO foobar:Pi7cYMZgj2VZQIMEAt2PF0AFBNtXU/Qjispf8aR7sHw=");


    }

}
