package nl.vpro.api.client.media;

import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.SocketException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.meeuw.functional.TriFunction;
import org.slf4j.event.Level;

import nl.vpro.api.client.Utils;
import nl.vpro.domain.Roles;
import nl.vpro.domain.media.EntityType;
import nl.vpro.logging.Slf4jHelper;
import nl.vpro.poms.shared.Headers;
import nl.vpro.rs.client.HeaderInterceptor;
import nl.vpro.rs.media.MediaBackendRestService;

/**
 * This Proxy:
 * - throttles all calls
 * - automatically fills some common arguments (recognized by @QueryParam annotations)
 * - if the return type is Response, it also checks the status code
 * - NotFoundException is wrapped to <code>null</code>
 * @author Michiel Meeuwissen
 * @since 4.3
 */
@Slf4j
class MediaRestClientAspect<T> implements InvocationHandler {

    private final MediaRestClient client;
    private final T proxied;
    private final TriFunction<Method, Object[], String, Level> headerLevel;


    MediaRestClientAspect(MediaRestClient client, T proxied, TriFunction<Method, Object[], String,  Level> headerLevel) {
        this.client = client;
        this.proxied = proxied;
        this.headerLevel = headerLevel;
    }

    @SuppressWarnings("unchecked")
    public static <T> T proxy(MediaRestClient client, T restController, Class<T> service) {
        return (T) Proxy.newProxyInstance(MediaRestClient.class.getClassLoader(),
            new Class[]{service}, new MediaRestClientAspect<>(client, restController, client.getHeaderLevel()));
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
                    dealWithHeaders(method, args);
                    log.debug("RESULT {}", result);
                    if (result instanceof Response){
                        if (dealWithResponse((Response) result, method)) {
                            continue;
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
                // lets retry retry

            } catch (javax.ws.rs.ProcessingException pe) {
                Throwable t = pe.getCause();
                if (t instanceof SocketException) {
                    client.retryAfterWaitOrException(method.getName() + ": SocketException: " + t.getMessage(), pe);// retry
                } else {
                    throw pe;
                }
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                cleanAfter();
            }
            // exception not rethrown, next iteration will try again
        }
    }

    protected void fillParametersIfEmpty(Method method, Object[] args) {
        Annotation[][] annotations = method.getParameterAnnotations();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                for (int j = 0; j < annotations[i].length; j++) {
                    if (annotations[i][j] instanceof QueryParam queryParam && args[i] == null) {
                        if (MediaBackendRestService.ERRORS.equals(queryParam.value())) {
                            log.debug("Implicitly set errors parameter to {}", client.errors);
                            args[i] = client.errors;
                        } else if (MediaBackendRestService.FOLLOW.equals(queryParam.value())) {
                            log.debug("Implicitly set followMerges to {}", client.isFollowMerges());
                            args[i] = client.isFollowMerges();
                        } else if (MediaBackendRestService.VALIDATE_INPUT.equals(queryParam.value())) {
                            log.debug("Implicitly set validateInput to {}", client.isValidateInput());
                            args[i] = client.isValidateInput();
                        } else if (MediaBackendRestService.OWNER.equals(queryParam.value())) {
                            if (client.getOwner() != null) {
                                log.debug("Implicitly set owner to {}", client.getOwner());
                                args[i] = client.getOwner().name();
                            }
                        } else if (MediaBackendRestService.PUBLISH.equals(queryParam.value())) {
                            if (client.isPublishImmediately()) {
                                log.debug("Implicitly set publish to {}", client.isPublishImmediately());
                                args[i] = client.isPublishImmediately();
                            }
                        } else if (MediaBackendRestService.DELETES.equals(queryParam.value())) {
                            if (client.getDeletes() != null) {
                                log.debug("Implicitly set deletes to {}", client.getDeletes());
                                args[i] = client.getDeletes();
                            }
                        }
                    }
                    if (annotations[i][j] instanceof PathParam pathParam && args[i] == null) {
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
                            log.debug("Implicitly set entity to {}", args[i]);
                        }
                    }

                    // This puts the actual value of a header param into thread local of interceptor
                    // This is because this interceptor is used to have a default value
                    // for the content type request header (normally application/xml)
                    // Some call do have an explicit Content type parameter though.
                    // By putting their value in the thread local, the interceptor will effectively be disabled.
                    if (annotations[i][j] instanceof HeaderParam headerParam) {
                        if (HttpHeaders.CONTENT_TYPE.equals(headerParam.value())) {
                            ContentTypeInterceptor.CONTENT_TYPE.set((String) args[i]);
                        }
                    }
                }

            }
        }
    }

    protected static void cleanAfter() {
         ContentTypeInterceptor.CONTENT_TYPE.remove();
    }

    protected void dealWithHeaders(Method method, Object[] args) {
        MultivaluedMap<String, String> headers = HeaderInterceptor.getHeaders();
        if (headers == null) {
            log.warn("No headers found");
        } else {
            List<String> warnings = headers.get(Headers.NPO_VALIDATION_WARNING_HEADER);
            if (warnings != null) {
                String methodString = Utils.methodCall(method, args);
                for (Object w : warnings) {
                    String asString = w + " (" + methodString + ")";
                    log.warn(asString);
                    client.getWarnings().add(asString);
                }
            }

            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                if (e.getKey().startsWith(Headers.X_NPO)) {
                    Level level = headerLevel.apply(method, args, e.getKey());
                    Slf4jHelper.log(log, level, "{}: {}", e.getKey(), e.getValue().stream().map(String::valueOf).collect(Collectors.joining(", ")));
                }
                if (e.getKey().equals(Headers.NPO_ROLES)) {
                    Set<String> copyOfRoles = new HashSet<>(client.roles);
                    client.roles.clear();
                    Stream.of(e.getValue().get(0)
                            .split("\\s*,\\s"))
                        .map(r -> r.substring(Roles.ROLE.length()))
                        .forEach(r -> client.roles.add(r));
                    if (!Objects.equals(copyOfRoles, client.roles)) {
                        log.info("Roles for client {}: {}", client, client.roles);
                    }
                }
            }
        }
    }


    protected boolean dealWithResponse(Response response, Method method) {
        log.debug("Dealing with {}", response);
        try {
            if (response.getStatusInfo() == Response.Status.SERVICE_UNAVAILABLE) {
                String message = response.readEntity(String.class);
                // retry
                client.retryAfterWaitOrException(method.getName() + " " + message, new ServiceUnavailableException(message));
                response.close();
                return  true;
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
            log.debug("dealt with response");
        }
        return false;
    }
}
