package nl.vpro.api.client.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.api.validation.ViolationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.util.concurrent.RateLimiter;

import nl.vpro.api.client.resteasy.PageUpdateApiClient;
import nl.vpro.domain.page.update.PageUpdate;
import nl.vpro.jackson2.Jackson2Mapper;

/**
 * @author Michiel Meeuwissen
 * @since 1.0
 */
public class PageUpdateApiClientUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PageUpdateApiClientUtil.class);



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

    private double baseRate = 1.0;
    private double minRate  = 0.01;

    private final RateLimiter limiter = RateLimiter.create(baseRate);


    @Inject
    public PageUpdateApiClientUtil(PageUpdateApiClient clients) {
        pageUpdateApiClient = clients;
    }

    public Result save(@NotNull @Valid PageUpdate update) {
        try {
            return handleResponse(pageUpdateApiClient.getPageUpdateRestService().save(update), update, JACKSON);
        } catch (Exception e) {
            return Result.error(pageUpdateApiClient + ":" + e.getMessage());
        }

    }

    public Result delete(@NotNull String id) {
        try {
            return handleResponse(pageUpdateApiClient.getPageUpdateRestService().delete(id, false), id, STRING);
        } catch (Exception e) {
            LOG.warn(e.getMessage());
            return Result.error(pageUpdateApiClient + ":" + e.getClass().getName() + " " + e.getMessage());
        }

    }


    public Result deleteWhereStartsWith(@NotNull String id) {
        try {
            return handleResponse(pageUpdateApiClient.getPageUpdateRestService().delete(id, true), id, STRING);
        } catch (Exception e) {
            return Result.error(pageUpdateApiClient + ":" + e.getMessage());
        }

    }

    private void upRate() {
        setRate(limiter.getRate() * 2);
    }
    private void downRate() {
        setRate(limiter.getRate() / 2);
    }

    private void setRate(double r) {
        if (r > baseRate) {
            r = baseRate;
        }
        if (r < minRate) {
            r = minRate;
        }
        limiter.setRate(r);
    }

    protected <T> Result handleResponse(Response response, T input, Function<Object, String> toString) {
        limiter.acquire();
        switch(response.getStatus()) {
            case 200:
                LOG.debug(pageUpdateApiClient + " " + response.getStatus());
                upRate();
                return Result.success();
            case 404:
                return Result.notfound("Not found error");
            default:
                downRate();
                MultivaluedMap<String, Object> headers = response.getHeaders();
                if ("true".equals(headers.getFirst("validation-exception"))) {
                    ViolationReport report = response.readEntity(ViolationReport.class);
                    String string = JACKSON.apply(report);
                    return Result.error(string);
                } else {
                    String string = pageUpdateApiClient + " " + response.getStatus() + " " + new HashMap<>(response.getStringHeaders()) + " " + response.getEntity() + " for: '" + toString.apply(input) + "'";
                    return Result.error(string);
                }
        }
    }
}
