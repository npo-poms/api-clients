/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import java.util.concurrent.TimeUnit;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import nl.vpro.resteasy.JacksonContextResolver;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class AbstractApiClient {

    static {
        ResteasyProviderFactory resteasyProviderFactory = ResteasyProviderFactory.getInstance();
        try {
            JacksonContextResolver jacksonContextResolver = new JacksonContextResolver();
            resteasyProviderFactory.registerProviderInstance(jacksonContextResolver);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }

        resteasyProviderFactory.addClientErrorInterceptor(new NpoApiClientErrorInterceptor());

        RegisterBuiltin.register(resteasyProviderFactory);
    }

    protected final ClientHttpEngine clientHttpEngine;

    public AbstractApiClient(int connectionTimeoutMillis, int maxConnections, int connectionInPoolTTL) {
        clientHttpEngine = buildHttpEngine(connectionTimeoutMillis, maxConnections, connectionInPoolTTL);
    }

    protected ApacheHttpClient4Engine buildHttpEngine(int connectionTimeoutMillis, int maxConnections, int connectionInPoolTTL) {
        PoolingHttpClientConnectionManager poolingClientConnectionManager = new PoolingHttpClientConnectionManager(connectionInPoolTTL, TimeUnit.MILLISECONDS);
        poolingClientConnectionManager.setDefaultMaxPerRoute(maxConnections);
        poolingClientConnectionManager.setMaxTotal(maxConnections);

        RequestConfig defaultRequestConfig = RequestConfig.custom()
            .setExpectContinueEnabled(true)
            .setStaleConnectionCheckEnabled(true)
            .setMaxRedirects(100)
            .setConnectionRequestTimeout(connectionTimeoutMillis)
            .build();

        CloseableHttpClient client = HttpClients.custom()
            .setConnectionManager(poolingClientConnectionManager)
            .setDefaultRequestConfig(defaultRequestConfig)
            .build();

        return new ApacheHttpClient4Engine(client);
    }
}
