package nl.vpro.api.client.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

//import org.apache.http.impl.execchain.RequestAbortedException;
import org.jboss.resteasy.api.validation.ViolationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.inject.Inject;

import nl.vpro.api.client.resteasy.PageUpdateApiClient;
import nl.vpro.domain.classification.ClassificationService;
import nl.vpro.domain.page.update.PageUpdate;
import nl.vpro.jackson2.Jackson2Mapper;

/**
 * @author Michiel Meeuwissen
 * @since 1.0
 */
public class PageUpdateApiUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PageUpdateApiUtil.class);



    private static final Function<Object, String> STRING = new Function<Object, String>() {
        @Override
        public String apply(@Nullable Object input) {
            return String.valueOf(input);

        }
    };
    private static final Function<Object, String> JACKSON = new Function<Object, String>() {
        @Override
        public String apply(@Nullable Object input) {
            StringWriter writer = new StringWriter();
            try {
                Jackson2Mapper.getInstance().writer().writeValue(writer, input);
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
            return writer.toString();
        }
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


    protected Result exceptionToResult(Exception e) {
        Throwable cause = e.getCause();
        if (cause.getClass().getName().indexOf("RequestAbortedException") > 0) {
            return Result.aborted(pageUpdateApiClient + ":" + e.getClass().getName() + " " + cause.getMessage());
        } else {
            limiter.downRate();
            LOG.warn(e.getMessage());
            return Result.error(pageUpdateApiClient + ":" + e.getClass().getName() + " " + e.getMessage());
        }
    }

    protected <T> Result handleResponse(Response response, T input, Function<Object, String> toString) {
        try {
            switch (response.getStatus()) {
                case 200:
                case 202:
                    LOG.debug(pageUpdateApiClient + " " + response.getStatus());
                    limiter.upRate();
                    return Result.success();
                case 400: {
                    String error = response.readEntity(String.class);
                    String s = pageUpdateApiClient + " " + response.getStatus() + " " + error;
                    limiter.upRate();
                    return Result.invalid(s);
                }
                case 404: {
                    String error = response.readEntity(String.class);
                    String s = pageUpdateApiClient + " " + response.getStatus() + " " + error;
                    limiter.downRate();
                    return Result.notfound(s);
                }
                case 403: {
                    String error = response.readEntity(String.class);
                    String s = pageUpdateApiClient + " " + response.getStatus() + " " + error;
                    limiter.upRate();
                    return Result.denied(s);
                }
                case 503: {
                    limiter.downRate();
                    String string = pageUpdateApiClient + " " + response.getStatus() + " " + input.toString();
                    return Result.error(string);
                }
                default: {
                    MultivaluedMap<String, Object> headers = response.getHeaders();
                    if ("true".equals(headers.getFirst("validation-exception"))) {
                        if ("text/plain".equals(headers.getFirst("Content-Type"))) {
                            String string = response.readEntity(String.class);
                            return Result.invalid(pageUpdateApiClient + ":" + string);
                        } else {
                            try {
                                ViolationReport report = response.readEntity(ViolationReport.class);
                                String string = JACKSON.apply(report);
                                return Result.invalid(pageUpdateApiClient + ":" + string);
                            } catch (Exception e) {
                                return Result.invalid(pageUpdateApiClient + ":" + String.valueOf(new HashMap<>(headers)) + "(" + e.getMessage() + ")");
                            }
                        }
                    } else {
                        limiter.downRate();
                        String error = response.readEntity(String.class);
                        String string = pageUpdateApiClient + " " + response.getStatus() + " " + new HashMap<>(response.getStringHeaders()) + " " + error + " for: '" + toString.apply(input) + "'";
                        return Result.error(string);
                    }
                }
            }
        } finally {
            response.close();
        }
    }
}
