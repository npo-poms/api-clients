package nl.vpro.api.client.resteasy;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.HttpHeaders;

import nl.vpro.api.rs.v3.media.MediaRestService;

/**
 * @author Michiel Meeuwissen
 * @since 5.3
 */
@Priority(100000)
@Slf4j
public class NpoApiClientsFilter  implements ClientResponseFilter {

    private final Set<Method> nocachingMethod = new HashSet<>();

    public NpoApiClientsFilter() throws NoSuchMethodException {
        nocachingMethod.add(MediaRestService.class.getMethod("changes", String.class, String.class, Long.class, Instant.class, String.class, Integer.class, Boolean.class, HttpServletRequest.class, HttpServletResponse.class));
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        CountAspect.Local local = CountAspect.currentThreadLocal.get();
        if (nocachingMethod.contains(local.method)) {
            log.debug("explicitely not caching call to {}", local);
            responseContext.getHeaders().putSingle(HttpHeaders.CACHE_CONTROL, "no-cache");
        }

    }
}