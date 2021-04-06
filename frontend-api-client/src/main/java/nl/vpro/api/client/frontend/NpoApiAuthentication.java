package nl.vpro.api.client.frontend;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import nl.vpro.api.client.utils.Util;

import static nl.vpro.poms.shared.Headers.NPO_DATE;

/**
 * Can performs authentication for NPO Frontend API.
 * E.g. like this it can be used in restassured.
 * <pre>
 * {@code
   import io.restassured.filter.Filter;
  import nl.vpro.api.client.frontend.NpoApiAuthentication;

   protected static final NpoApiAuthentication authentication = new NpoApiAuthentication(
          CONFIG.requiredOption(npo_api, "apiKey"),
          CONFIG.requiredOption(npo_api, "secret"),
          CONFIG.requiredOption(npo_api, "origin"));


   public static final Filter RESTEASSURED_AUTHENTICATION =
          (requestSpec, responseSpec, filterContext) -> {
          Map<String, Object> authenticate =  authentication.authenticate(URI.create(requestSpec.getURI()));
          for(Map.Entry<String, Object> e : authenticate.entrySet()) {
              requestSpec.header(new Header(e.getKey(), e.getValue().toString()));
          }

          return filterContext.next(requestSpec, responseSpec);

      };
  }
 * </pre>
 * It is also used in {@link ApiAuthenticationRequestFilter} for authentication in resteasy clients.
 *
 * @author Michiel Meeuwissen
 * @since 1.11
 */
public class NpoApiAuthentication {

    private final String apiKey;

    private final String secret;

    private final String origin;

    public NpoApiAuthentication(String apiKey, String secret, String origin) {
        this.apiKey = apiKey;
        this.secret = secret;
        this.origin = origin;
    }

    public Map<String, Object> authenticate(URI uri) {
        String now = Util.rfc822(new Date());

        String message = message(uri, now);

        Map<String, Object> headers = new TreeMap<>();
        if (secret == null) {
            throw new IllegalStateException("No npo api secret found");
        }
        if (apiKey == null) {
            throw new IllegalStateException("No npo api apiKey found");
        }
        if (origin == null) {
            throw new IllegalStateException("No npo api origin found");
        }
        headers.put("Authorization", "NPO " + apiKey + ':' + Util.hmacSHA256(secret, message));
        headers.put("Origin", origin);
        headers.put(NPO_DATE, now);
        return headers;
    }


    private String message(URI uri, String now) {

        StringBuilder sb = new StringBuilder();
        sb.append("origin:").append(origin).append(',');

        sb.append("x-npo-date:").append(now).append(',');

        sb.append("uri:").append(uri.getRawPath());

        String q = uri.getQuery();
        final List<NameValuePair> query;
        if (q != null) {
            query = URLEncodedUtils.parse(uri.getQuery(), StandardCharsets.UTF_8);
        } else {
            query = new ArrayList<>();
        }

        SortedSet<NameValuePair> sortedQuery = new TreeSet<>(
            Comparator.comparing(NameValuePair::getName)
                .thenComparing(NameValuePair::getValue)
        );

        sortedQuery.addAll(query);

        for (NameValuePair pair : sortedQuery) {
            sb.append(',').append(pair.getName()).append(':').append(pair.getValue());
        }

        return sb.toString();
    }
}
