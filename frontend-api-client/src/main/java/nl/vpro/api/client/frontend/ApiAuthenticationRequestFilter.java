/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.frontend;

import java.net.URI;
import java.util.Map;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class ApiAuthenticationRequestFilter implements ClientRequestFilter {

    private final NpoApiAuthentication authentication;

    public ApiAuthenticationRequestFilter(String apiKey, String secret, String origin) {
        authentication = new NpoApiAuthentication(apiKey, secret, origin);
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        authenticate(requestContext.getUri(), requestContext.getHeaders());
    }

    public void authenticate(URI uri, MultivaluedMap<String, Object> headers) {

        for (Map.Entry<String, Object> entry : authentication.authenticate(uri).entrySet()) {
            headers.add(entry.getKey(), entry.getValue());
        }
    }

}
