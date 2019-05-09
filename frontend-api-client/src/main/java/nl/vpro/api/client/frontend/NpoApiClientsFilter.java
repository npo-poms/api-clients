package nl.vpro.api.client.frontend;

import lombok.extern.slf4j.Slf4j;
import nl.vpro.api.client.resteasy.CountAspect;
import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.domain.api.Deletes;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Michiel Meeuwissen
 * @since 5.3
 */
@Priority(100000)
@Slf4j
public class NpoApiClientsFilter  implements ClientResponseFilter {

    private final Set<Method> nocachingMethod = new HashSet<>();

    public NpoApiClientsFilter() throws NoSuchMethodException {
        nocachingMethod.add(
            MediaRestService.class.getMethod("changes",
                String.class,
                String.class,
                Long.class,
                String.class,
                String.class,
                Integer.class,
                Boolean.class,
                Deletes.class,
                HttpServletRequest.class,
                HttpServletResponse.class));
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        CountAspect.Local local = CountAspect.currentThreadLocal.get();
        if (nocachingMethod.contains(local.getMethod())) {
            log.debug("explicitely not caching call to {}", local);
            responseContext.getHeaders().putSingle(HttpHeaders.CACHE_CONTROL, "no-cache");
        }

    }
}
