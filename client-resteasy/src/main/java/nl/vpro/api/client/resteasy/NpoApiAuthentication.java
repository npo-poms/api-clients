/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class NpoApiAuthentication implements ClientRequestFilter {

    private final String apiKey;

    private final String secret;

    private final String origin;

    public NpoApiAuthentication(String apiKey, String secret, String origin) {
        this.apiKey = apiKey;
        this.secret = secret;
        this.origin = origin;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        authenticate(requestContext.getUri(), requestContext.getHeaders());
    }

    public void authenticate(URI uri, MultivaluedMap<String, Object> headers) {
        String now = Util.rfc822(new Date());

        String message = message(uri, now);

        headers.add("Authorization", "NPO " + apiKey + ':' + Util.hmacSHA256(secret, message));
        headers.add("Origin", origin);
        headers.add("X-NPO-Date", now);
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
                if(i != 0) {
                    return i;
                }
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        sortedQuery.addAll(query);

        for(NameValuePair pair : sortedQuery) {
            sb.append(',').append(pair.getName()).append(':').append(pair.getValue());
        }

        return sb.toString();
    }
}
