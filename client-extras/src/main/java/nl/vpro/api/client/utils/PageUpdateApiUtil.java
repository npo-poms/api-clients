package nl.vpro.api.client.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.StringWriter;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;

import jakarta.inject.Inject;
import  jakarta.validation.Valid;
import  jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.impl.execchain.RequestAbortedException;

import nl.vpro.api.client.pages.PageUpdateApiClient;
import nl.vpro.domain.classification.ClassificationService;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.page.Page;
import nl.vpro.domain.page.PageIdMatch;
import nl.vpro.domain.page.update.*;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.rs.client.Utils;

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

    @Getter
    @Setter
    private boolean retryErrors = true;

    @Inject
    @lombok.Builder
    public PageUpdateApiUtil(
        PageUpdateApiClient client,
        PageUpdateRateLimiter limiter) {
        pageUpdateApiClient = client;
        this.limiter = limiter == null ?  PageUpdateRateLimiter.builder().build() : limiter;
    }

    public Result<Void> save(@NotNull @Valid PageUpdate update) {
        return save(update, false);
    }

    public Result<Void> saveAndWait(@NotNull @Valid PageUpdate update) {
        return save(update, true);
    }

    protected Result<Void> save(@NotNull @Valid PageUpdate update, boolean wait) {
        while(true) {
            limiter.acquire();
            try {

                SaveResult result = pageUpdateApiClient.getPageUpdateRestService().save(update, wait);
                limiter.upRate();
                return Result.success(null);

            } catch (ProcessingException e) {
                limiter.downRate();
                return exceptionToResult(e);
            }
        }
    }


    public PageUpdate get(@NotNull String url) {
        limiter.acquire();
        PageIdMatch match = url.startsWith("crid:") ? PageIdMatch.CRID : PageIdMatch.URL;
        return Utils.wrapNotFound(() -> pageUpdateApiClient.getPageUpdateRestService().load(url, false, match)).orElse(null);
    }

    public DeleteResult delete(@NotNull String id) {
        limiter.acquire();
        PageIdMatch match = id.startsWith("crid:") ? PageIdMatch.CRID : PageIdMatch.URL;
        return pageUpdateApiClient.getPageUpdateRestService()
            .delete(id, false, 1, false, match);

    }


    public DeleteResult deleteWhereStartsWith(@NotNull String prefix) {
        limiter.acquire();
        PageIdMatch match = prefix.startsWith("crid:") ? PageIdMatch.CRID : PageIdMatch.URL;

        int batchSize = 10000;
        DeleteResult result = null;
        while (true) {
            DeleteResult r = pageUpdateApiClient.getPageUpdateRestService()
                .delete(prefix, true, batchSize, true, match);
            log.info("Batch deleted {}: {}", prefix, r);
            if (result != null) {
                result = result.and(r);
            } else {
                result = r;
            }
            if (r.getCount() == 0) {
                return result;
            }
        }
    }

    public Optional<Page> getPublishedPage(String url) {
        try {
            return Optional.of(getPageUpdateApiClient().getProviderRestService().getPage(url));
        } catch (NotFoundException nfe) {
            return Optional.empty();
        }
    }

    public Optional<MediaObject> getMedia(String mid) {
        return Optional.of(getPageUpdateApiClient().getProviderRestService().getMedia(mid));

    }

    public ClassificationService getClassificationService() {
        return pageUpdateApiClient.getClassificationService();
    }

    public PageUpdateApiClient getPageUpdateApiClient() {
        return pageUpdateApiClient;
    }

    protected <E> Result<E> exceptionToResult(Exception e) {
        Throwable cause = ExceptionUtils.getRootCause(e);
        try {
            throw cause;
        } catch (RequestAbortedException rae) {
            return returnResult(Result.aborted(pageUpdateApiClient + ":" + cause.getClass().getName() + " " + cause.getMessage()));
        } catch (SocketException se) {
            return returnResult(Result.error(pageUpdateApiClient + ":" + cause.getClass().getName() + " " + cause.getMessage()));
        } catch (ProcessingException | NullPointerException fatal) {
            return returnResult(Result.fatal(pageUpdateApiClient + ":" + e.getClass().getName() + " " + e.getMessage(), e));
        } catch (Throwable t) {
            return returnResult(Result.error(pageUpdateApiClient + ":" + e.getClass().getName() + " " + e.getMessage()));
        }
    }

    protected <T, E> Result<E> handleResponse(
        Response response,
        T input,
        Function<Object, String> toString,
        Class<E> e) {
        try (response) {
            switch (response.getStatus()) {
                case 200, 202 -> {
                    log.debug(pageUpdateApiClient + " " + response.getStatus());
                    try {
                        E entity = response.readEntity(e);
                        return returnResult(Result.success(entity));
                    } catch (Exception ex) {
                        log.error("For {}: {}", response.getEntity(), ex.getMessage(), ex);
                        throw ex;
                    }
                }
                case 400 -> {
                    String error = response.readEntity(String.class);
                    String s = pageUpdateApiClient + " " + response.getStatus() + " " + error;
                    return returnResult(Result.invalid(s));
                }
                case 404 -> {
                    String error = response.readEntity(String.class);
                    String s = pageUpdateApiClient + " " + response.getStatus() + " " + error;
                    return returnResult(Result.notfound(s));
                }
                case 403 -> {
                    String error = response.readEntity(String.class);
                    String s = pageUpdateApiClient + " " + response.getStatus() + " " + error;
                    return returnResult(Result.denied(s));
                }
                case 503 -> {
                    String string = pageUpdateApiClient + " " + response.getStatus() + " " + input.toString();
                    return returnResult(Result.error(string));
                }
                default -> {
                    MultivaluedMap<String, Object> headers = response.getHeaders();
                    if ("true".equals(headers.getFirst("validation-exception"))) {
                        if ("text/plain".equals(headers.getFirst("Content-Type"))) {
                            String string = response.readEntity(String.class);
                            return returnResult(Result.invalid(pageUpdateApiClient + ":" + string));
                        } else {
                            try {
                                String string = response.readEntity(String.class);
                                return returnResult(Result.invalid(pageUpdateApiClient + ":" + string));
                            } catch (Exception ex) {
                                return returnResult(Result.invalid(pageUpdateApiClient + ":" + new HashMap<>(headers) + "(" + ex.getMessage() + ")"));
                            }
                        }
                    } else {
                        String error = response.readEntity(String.class);
                        String string = pageUpdateApiClient + " " + response.getStatus() + " " + new HashMap<>(response.getStringHeaders()) + " " + error + " for: '" + toString.apply(input) + "'";
                        return returnResult(Result.error(string));
                    }
                }
            }
        }
    }

    protected <E> Result<E> returnResult(Result<E> result) {
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
