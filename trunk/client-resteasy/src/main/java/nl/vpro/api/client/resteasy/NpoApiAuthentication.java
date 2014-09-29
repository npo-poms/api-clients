/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

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

        String now = Util.rfc822(new Date());

        String message = message(requestContext, now);

        requestContext.getHeaders().add("Authorization", "NPO " + apiKey + ':' + Util.hmacSHA256(secret, message));
        requestContext.getHeaders().add("Origin", origin);
        requestContext.getHeaders().add("X-NPO-Date", now);
    }

    private String message(ClientRequestContext requestContext, String now) {

        StringBuilder sb = new StringBuilder();
        sb.append("origin:").append(origin).append(',');

        sb.append("x-npo-date:").append(now).append(',');

        String uri = requestContext.getUri().getPath();
        sb.append("uri:").append(uri);

        List<NameValuePair> query = URLEncodedUtils.parse(requestContext.getUri().getQuery(), Charset.forName("utf-8"));

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
