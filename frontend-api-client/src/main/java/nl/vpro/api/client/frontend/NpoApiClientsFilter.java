package nl.vpro.api.client.frontend;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Priority;
import javax.ws.rs.client.*;
import javax.ws.rs.core.HttpHeaders;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.api.rs.v3.page.PageRestService;
import nl.vpro.domain.api.Deletes;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.page.PageForm;
import nl.vpro.jmx.CountAspect;

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
                Deletes.class
            ));
        nocachingMethod.add(
            MediaRestService.class.getMethod("iterate",
                MediaForm.class,
                String.class,
                String.class,
                Long.class,
                Integer.class)
        );
        nocachingMethod.add(
            PageRestService.class.getMethod("iterate",
                PageForm.class,
                String.class,
                String.class,
                Long.class,
                Integer.class)
        );
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        CountAspect.Local local = CountAspect.currentThreadLocal.get();
        if (nocachingMethod.contains(local.getMethod())) {
            log.debug("explicitely not caching call to {}", local);
            responseContext.getHeaders()
                .putSingle(HttpHeaders.CACHE_CONTROL, "no-cache");
        }

    }
}
