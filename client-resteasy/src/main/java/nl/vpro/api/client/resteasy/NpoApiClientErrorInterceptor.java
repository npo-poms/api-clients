/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.ClientErrorInterceptor;

/**
 * See {@link ErrorAspect} for a more straightforward implementation
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
//@ClientInterceptor
//@Provider
public class NpoApiClientErrorInterceptor implements ClientErrorInterceptor {

    @Override
    public void handle(ClientResponse<?> response) throws RuntimeException {
        // Never got called...
        System.out.println("Hallelujah");
    }
}
