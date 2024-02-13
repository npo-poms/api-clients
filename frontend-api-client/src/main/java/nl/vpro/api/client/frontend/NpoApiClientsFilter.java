package nl.vpro.api.client.frontend;

import jakarta.annotation.Priority;
import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.HttpHeaders;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.api.rs.v3.page.PageRestService;
import nl.vpro.jmx.CountAspect;

/**
 * @author Michiel Meeuwissen
 * @since 5.3
 */
@Priority(100000)
@Slf4j
public class NpoApiClientsFilter  implements ClientResponseFilter {

    private final Set<Method> nocachingMethod = new HashSet<>();

    public NpoApiClientsFilter() {
        nocachingMethod.add(getNonCachingMethod(MediaRestService.class, "changes"));
        nocachingMethod.add(getNonCachingMethod(MediaRestService.class, "iterate"));
        nocachingMethod.add(getNonCachingMethod(PageRestService.class, "iterate"));
    }

    Method getNonCachingMethod(Class<?> clazz, String name) {
        Method found = null;
        for (Method declaredMethod : clazz.getDeclaredMethods()) {
            if (declaredMethod.getName().equals(name)) {
                if (found != null) {
                    throw new IllegalStateException();
                }
                found =  declaredMethod;
            }
        }
        if (found == null) {
            throw new IllegalStateException();
        }
        log.info("Will not cache for {}", found);
        return found;
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        CountAspect.Local local = CountAspect.currentThreadLocal.get();
        if (nocachingMethod.contains(local.getMethod())) {
            log.debug("explicitly not caching call to {}", local);
            responseContext.getHeaders()
                .putSingle(HttpHeaders.CACHE_CONTROL, "no-cache");
        }

    }
}
