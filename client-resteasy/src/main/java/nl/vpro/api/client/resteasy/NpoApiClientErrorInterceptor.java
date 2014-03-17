/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.ClientErrorInterceptor;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class NpoApiClientErrorInterceptor implements ClientErrorInterceptor {

    @Override
    public void handle(ClientResponse<?> response) throws RuntimeException {
        // Never got called...
        System.out.println("sadasa");
    }
}
