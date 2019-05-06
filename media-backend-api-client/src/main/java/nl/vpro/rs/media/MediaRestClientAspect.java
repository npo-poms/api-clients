package nl.vpro.rs.media;

import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import nl.vpro.domain.media.EntityType;

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
class MediaRestClientAspect<T> implements InvocationHandler {

    private final MediaRestClient client;
    private final T proxied;

    MediaRestClientAspect(MediaRestClient client, T proxied) {
        this.client = client;
        this.proxied = proxied;
    }

    public static <T> T proxy(MediaRestClient client, T restController, Class<T> service) {
        return (T) Proxy.newProxyInstance(MediaRestClient.class.getClassLoader(),
            new Class[]{service}, new MediaRestClientAspect(client, restController));
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
                                response.close();
                                continue;
                            }
                            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {

                                ResponseError error = new ResponseError(
                                    client.toString(),
                                    method,
                                    response.getStatus(),
                                    response.getStatusInfo(),
                                    response.readEntity(String.class)
                                );
                                response.close();
                                throw error;
                            }
                        } finally {

                        }


                    }
                    return result;
                } catch (InvocationTargetException itc) {
                    throw itc.getCause();
                }
            } catch (NotFoundException nfe) {
                return null;
            } catch (ServiceUnavailableException sue) {
                client.retryAfterWaitOrException(method.getName() + ": Service unavailable:" + sue.getMessage(), sue);
                // retry
                continue;
            } catch (InternalServerErrorException isee) {
                // odd, should not happen.
/*
) 500:<!DOCTYPE HTML PUBLIC "-//IETF//DTD HTML 2.0//EN">
<html><head>
<title>500 Internal Server Error</title>
</head><body>
<h1>Internal Server Error</h1>
<p>The server encountered an internal error or
misconfiguration and was unable to complete
your request.</p>
*/
                client.retryAfterWaitOrException(method.getName() + ": Internal Server error: " + isee.getMessage(), isee);
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
                            if (CharSequence.class.isAssignableFrom(method.getParameterTypes()[i])) {
                                args[i] = "media";
                            } else if (EntityType.class.isAssignableFrom(method.getParameterTypes()[i])) {
                                try {
                                    args[i] = method.getParameterTypes()[i].getDeclaredField("media").get(null);
                                } catch (Exception e) {
                                    log.error(e.getMessage());
                                }
                            }
                            log.debug("Implicetely set entity to {}", args[i]);
                        }


                    }
                    if (annotations[i][j] instanceof HeaderParam) {
                        HeaderParam headerParam = (HeaderParam) annotations[i][j];
                        if (HttpHeaders.CONTENT_TYPE.equals(headerParam.value())) {
                            ContentTypeInterceptor.CONTENT_TYPE.set((String) args[i]);
                        }

                    }
                }

            }
        }

    }
}
