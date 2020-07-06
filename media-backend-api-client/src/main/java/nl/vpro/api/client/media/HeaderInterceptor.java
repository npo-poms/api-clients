package nl.vpro.api.client.media;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Slf4j
public class HeaderInterceptor  implements ReaderInterceptor {

    static ThreadLocal<MultivaluedMap<String, String>> HEADERS = ThreadLocal.withInitial(() -> null);
    final MediaRestClient client;

    public HeaderInterceptor(MediaRestClient client) {
        this.client = client;
    }

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        HEADERS.set(context.getHeaders());
        return context.proceed();
    }
}
