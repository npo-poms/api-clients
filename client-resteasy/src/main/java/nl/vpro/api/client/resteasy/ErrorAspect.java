package nl.vpro.api.client.resteasy;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.function.Supplier;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.domain.api.Error;
import nl.vpro.jackson2.Jackson2Mapper;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;

/**
 * Wraps all calls to log client errors.
 * @author Michiel Meeuwissen
 * @since 4.2
 */

public class ErrorAspect<T> implements InvocationHandler {

    private final Logger log;

    private final T proxied;

    private final Supplier<String> string;

    ErrorAspect(T proxied, Logger log, Supplier<String> string) {
        this.proxied = proxied;
        this.log = log;
        this.string = string;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            try {
                return method.invoke(proxied, args);
            } catch (InvocationTargetException itc) {
                Throwable cause = itc.getCause();
                throw cause;
            }
        } catch (WebApplicationException b) {
            String mes;
            try {
                Response response = b.getResponse();
                response.bufferEntity();
                try {
                    Error error = response.readEntity(Error.class);
                    mes = error.toString();
                } catch (Exception e) {
                    String m = response.readEntity(String.class);
                    mes = response.getStatus() + ":" + m;
                }
            } catch (Exception e) {
                log.warn(e.getClass() + " " + e.getMessage());
                mes = b.getMessage();
            }


            log.error("Bad request for {}{}(\n{}\n) {}",
                string.get(),
                method.getDeclaringClass().getSimpleName() + "#" + method.getName(),
                Arrays.asList(args).stream().map(ErrorAspect.this::valueToString).collect(joining("\n")),
                mes);
            throw b;
        }
    }

    protected String valueToString(Object o) {
        if (o instanceof String) {
            return o.toString();
        } else {
            try {
                return Jackson2Mapper.getInstance().writeValueAsString(o);
            } catch (JsonProcessingException e) {

            }
            return o.toString();
        }
    }


    public static <T> T proxyErrors(Logger logger, Supplier<String> info, Class<T> inter, T service) {
        return (T) Proxy.newProxyInstance(inter.getClassLoader(), new Class[]{inter}, new ErrorAspect<T>(service, logger, info));
    }
}

