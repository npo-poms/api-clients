package nl.vpro.rs.media;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Proxy:
 * - throttles all calls
 * - automaticly fills some common arguments (recognized by @QueryParam annotations)
 * - if the return type is Response, it also checks the status code
 * @author Michiel Meeuwissen
 * @since 4.3
 */
class MediaRestClientAspect implements InvocationHandler {

    private static Logger LOG = LoggerFactory.getLogger(MediaRestClientAspect.class);

    private final MediaRestClient client;
    private final MediaBackendRestService proxied;

    MediaRestClientAspect(MediaRestClient client, MediaBackendRestService proxied) {
        this.client = client;
        this.proxied = proxied;
    }

    public static MediaBackendRestService proxy(MediaRestClient client, MediaBackendRestService restController) {
        return (MediaBackendRestService) Proxy.newProxyInstance(MediaRestClient.class.getClassLoader(),
            new Class[]{MediaBackendRestService.class}, new MediaRestClientAspect(client, restController));
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        while (true) {
            LOG.debug("Throttling {} (rate: {})", method, client.getThrottleRate());
            client.throttle();
            try {
                try {
                    fillErrorParameterIfEmpty(method, args);
                    Object result = method.invoke(proxied, args);
                    if (result instanceof Response) {
                        Response response = (Response) result;
                        if (response.getStatusInfo() == Response.Status.SERVICE_UNAVAILABLE) {
                            String message = response.readEntity(String.class);
                            // retry
                            client.retryAfterWaitOrException(method.getName() + " " + message);
                            continue;
                        }
                        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                            throw new ResponseError(client.toString(), method, response.getStatus(), response.getStatusInfo(), response.readEntity(String.class));
                        }

                    }
                    return result;
                } catch (InvocationTargetException itc) {
                    throw itc.getCause();
                }
            } catch (NotFoundException nfe) {
                return null;
            } catch (ServiceUnavailableException sue) {
                client.retryAfterWaitOrException(method.getName() + ": Service unavailable");
                // retry
                continue;
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void fillErrorParameterIfEmpty(Method method, Object[] args) {
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < args.length; i++) {
            for (int j = 0; j < annotations[i].length; j++) {
                if (annotations[i][j] instanceof QueryParam && args[i] == null) {
                    QueryParam queryParam = (QueryParam) annotations[i][j];
                    if ("errors".equals(queryParam.value())) {
                        LOG.debug("Implicetely set errors parameter to {}", client.errors);
                        args[i] = client.errors;
                    }
                    if ("followMerges".equals(queryParam.value())) {
                        LOG.debug("Implicetely set followMerges to {}", client.isFollowMerges());
                        args[i] = client.isFollowMerges();
                    }

                }
            }

        }

    }
}
