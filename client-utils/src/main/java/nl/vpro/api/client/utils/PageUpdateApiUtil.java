package nl.vpro.api.client.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.StringWriter;
import java.net.SocketException;
import java.util.HashMap;
import java.util.function.Function;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.http.impl.execchain.RequestAbortedException;

import nl.vpro.api.client.resteasy.PageUpdateApiClient;
import nl.vpro.api.client.resteasy.Utils;
import nl.vpro.domain.classification.ClassificationService;
import nl.vpro.domain.page.update.PageUpdate;
import nl.vpro.jackson2.Jackson2Mapper;

/**
 * @author Michiel Meeuwissen
 * @since 1.0
 */
@Slf4j
public class PageUpdateApiUtil {

    private static final Function<Object, String> STRING = String::valueOf;
    private static final Function<Object, String> JACKSON = input -> {
        StringWriter writer = new StringWriter();
        try {
            Jackson2Mapper.getInstance().writer().writeValue(writer, input);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return writer.toString();
    };

    private final PageUpdateApiClient pageUpdateApiClient;
    private final PageUpdateRateLimiter limiter;

    @Inject
    public PageUpdateApiUtil(PageUpdateApiClient clients, PageUpdateRateLimiter limiter) {
        pageUpdateApiClient = clients;
        this.limiter = limiter;
    }

    public Result save(@NotNull @Valid PageUpdate update) {
        limiter.acquire();
        try {
            return handleResponse(pageUpdateApiClient.getPageUpdateRestService().save(update), update, JACKSON);
        } catch (ProcessingException e) {
            return exceptionToResult(e);
        }
    }


    public PageUpdate get(@NotNull String url) {
        limiter.acquire();
        return Utils.wrapNotFound(() -> pageUpdateApiClient.getPageUpdateRestService().load(url)).orElse(null);
    }

    public Result delete(@NotNull String id) {
        limiter.acquire();
        try {
            return handleResponse(pageUpdateApiClient.getPageUpdateRestService().delete(id, false, 1), id, STRING);
        } catch (ProcessingException e) {
            return exceptionToResult(e);
        }
    }

    public Result deleteWhereStartsWith(@NotNull String id) {
        limiter.acquire();
        try {
            return handleResponse(pageUpdateApiClient.getPageUpdateRestService().delete(id, true, 10000), id, STRING);
        } catch (ProcessingException e) {
            return exceptionToResult(e);
        }
    }

    public ClassificationService getClassificationService() {
        return pageUpdateApiClient.getClassificationService();
    }

    public PageUpdateApiClient getPageUpdateApiClient() {
        return pageUpdateApiClient;
    }

    protected Result exceptionToResult(Exception e) {
        Throwable cause = e.getCause();
        if (cause instanceof RequestAbortedException) {
            return returnResult(Result.aborted(pageUpdateApiClient + ":" + cause.getClass().getName() + " " + cause.getMessage()));
        } else if (cause instanceof SocketException) {
            return returnResult(Result.error(pageUpdateApiClient + ":" + cause.getClass().getName() + " " + cause.getMessage()));
        } else {
            return returnResult(Result.error(pageUpdateApiClient + ":" + e.getClass().getName() + " " + e.getMessage()));
        }
    }

    protected <T> Result handleResponse(Response response, T input, Function<Object, String> toString) {
        try {
            switch (response.getStatus()) {
                case 200:
                case 202:
                    log.debug(pageUpdateApiClient + " " + response.getStatus());
                    return returnResult(Result.success());
                case 400: {
                    String error = response.readEntity(String.class);
                    String s = pageUpdateApiClient + " " + response.getStatus() + " " + error;
                    return returnResult(Result.invalid(s));
                }
                case 404: {
                    String error = response.readEntity(String.class);
                    String s = pageUpdateApiClient + " " + response.getStatus() + " " + error;
                    return returnResult(Result.notfound(s));
                }
                case 403: {
                    String error = response.readEntity(String.class);
                    String s = pageUpdateApiClient + " " + response.getStatus() + " " + error;
                    return returnResult(Result.denied(s));
                }
                case 503: {
                    String string = pageUpdateApiClient + " " + response.getStatus() + " " + input.toString();
                    return returnResult(Result.error(string));
                }
                default: {
                    MultivaluedMap<String, Object> headers = response.getHeaders();
                    if ("true".equals(headers.getFirst("validation-exception"))) {
                        if ("text/plain".equals(headers.getFirst("Content-Type"))) {
                            String string = response.readEntity(String.class);
                            return returnResult(Result.invalid(pageUpdateApiClient + ":" + string));
                        } else {
                            try {
                                String string = response.readEntity(String.class);
                                return returnResult(Result.invalid(pageUpdateApiClient + ":" + string));
                            } catch (Exception e) {
                                return returnResult(Result.invalid(pageUpdateApiClient + ":" + String.valueOf(new HashMap<>(headers)) + "(" + e.getMessage() + ")"));
                            }
                        }
                    } else {
                        String error = response.readEntity(String.class);
                        String string = pageUpdateApiClient + " " + response.getStatus() + " " + new HashMap<>(response.getStringHeaders()) + " " + error + " for: '" + toString.apply(input) + "'";
                        return returnResult(Result.error(string));
                    }
                }
            }
        } finally {
            response.close();
        }
    }

    protected Result returnResult(Result result) {
        if (result.needsRetry()) {
            limiter.downRate();
        }
        if (result.isOk()) {
            limiter.upRate();
        } else {
            log.debug(result.getErrors());
        }
        return result;
    }

    @Override
    public String toString() {
        return "PageUpdateApiUtil for " + pageUpdateApiClient.getDescription();
    }
}
