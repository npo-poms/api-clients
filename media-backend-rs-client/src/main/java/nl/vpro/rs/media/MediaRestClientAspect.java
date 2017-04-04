package nl.vpro.rs.media;

import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.Response;

/**
 * This Proxy:
 * - throttles all calls
 * - automaticly fills some common arguments (recognized by @QueryParam annotations)
 * - if the return type is Response, it also checks the status code
 * - NotFoundException is wrapped to <code>null</code>
 * @author Michiel Meeuwissen
 * @since 4.3
 */
@Slf4j
class MediaRestClientAspect implements InvocationHandler {

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
            log.debug("Throttling {} (rate: {})", method, client.getThrottleRate());
            client.throttle();
            try {
                try {
                    fillParametersIfEmpty(method, args);
                    Object result = method.invoke(proxied, args);
                    if (result instanceof Response) {
                        Response response = (Response) result;
                        try {
                            if (response.getStatusInfo() == Response.Status.SERVICE_UNAVAILABLE) {
                                String message = response.readEntity(String.class);
                                // retry
                                client.retryAfterWaitOrException(method.getName() + " " + message, new ServiceUnavailableException(message));
                                continue;
                            }
                            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                                throw new ResponseError(client.toString(), method, response.getStatus(), response.getStatusInfo(), response.readEntity(String.class));
                            }
                        } finally {
/*
                            if (buffered) {
                                response.close();
                            }
*/
                        }


                    }
                    return result;
                } catch (InvocationTargetException itc) {
                    throw itc.getCause();
                }
            } catch (NotFoundException nfe) {
                return null;
            } catch (ServiceUnavailableException sue) {
                client.retryAfterWaitOrException(method.getName() + ": Service unavailable", sue);
                // retry
                continue;
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void fillParametersIfEmpty(Method method, Object[] args) {
        Annotation[][] annotations = method.getParameterAnnotations();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                for (int j = 0; j < annotations[i].length; j++) {
                    if (annotations[i][j] instanceof QueryParam && args[i] == null) {
                        QueryParam queryParam = (QueryParam) annotations[i][j];
                        if (MediaBackendRestService.ERRORS.equals(queryParam.value())) {
                            log.debug("Implicetely set errors parameter to {}", client.errors);
                            args[i] = client.errors;
                        } else if (MediaBackendRestService.FOLLOW.equals(queryParam.value())) {
                            log.debug("Implicetely set followMerges to {}", client.isFollowMerges());
                            args[i] = client.isFollowMerges();
                        } else if (MediaBackendRestService.VALIDATE_INPUT.equals(queryParam.value())) {
                            log.debug("Implicetely set validateInput to {}", client.isValidateInput());
                            args[i] = client.isValidateInput();
                        }
                    }
                    if (annotations[i][j] instanceof PathParam && args[i] == null) {
                        PathParam pathParam = (PathParam) annotations[i][j];
                        if ("entity".equals(pathParam.value())) {
                            log.debug("Implicetely set entity to media");
                            args[i] = "media";
                        }

                    }
                }

            }
        }

    }
}
