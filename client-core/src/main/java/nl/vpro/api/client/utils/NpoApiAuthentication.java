package nl.vpro.api.client.utils;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

/**
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
        headers.put("Authorization", "NPO " + apiKey + ':' + Util.hmacSHA256(secret, message));
        headers.put("Origin", origin);
        headers.put("X-NPO-Date", now);
        return headers;
    }


    private String message(URI uri, String now) {

        StringBuilder sb = new StringBuilder();
        sb.append("origin:").append(origin).append(',');

        sb.append("x-npo-date:").append(now).append(',');

        sb.append("uri:").append(uri.getRawPath());

        List<NameValuePair> query = URLEncodedUtils.parse(uri.getQuery(), Charset.forName("utf-8"));

        SortedSet<NameValuePair> sortedQuery = new TreeSet<>(new Comparator<NameValuePair>() {
            @Override
            public int compare(NameValuePair o1, NameValuePair o2) {
                int i = o1.getName().compareTo(o2.getName());
                if (i != 0) {
                    return i;
                }
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        sortedQuery.addAll(query);

        for (NameValuePair pair : sortedQuery) {
            sb.append(',').append(pair.getName()).append(':').append(pair.getValue());
        }

        return sb.toString();
    }
}
