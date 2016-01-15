package nl.vpro.api.client.resteasy;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.domain.api.Error;
import nl.vpro.jackson2.Jackson2Mapper;

import static java.util.stream.Collectors.joining;

/**
 * Wraps all calls to log client errors.
 *
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
        Throwable t;
        String mes;
        try {
            try {
                return method.invoke(proxied, args);
            } catch (InvocationTargetException itc) {
                throw itc.getCause();
            }
        } catch (WebApplicationException wea) {
            mes = getMessage(wea);
            t = wea;
        } catch (Throwable e) {
            mes = e.getClass().getName() + " " + e.getMessage();
            t = e;
        }
        log.error("Error for {}{}(\n{}\n) {}",
            string.get(),
            method.getDeclaringClass().getSimpleName() + "#" + method.getName(),
            Arrays.asList(args).stream().map(ErrorAspect.this::valueToString).collect(joining("\n")),
            mes);
        throw t;
    }

    protected static final String[] HEADERS = new String[] {
        "Set-Cookie", // may give information about which backend server was used
        "X-ProxyInstancename",
        "Content-Type" // problem may be related to json vs xml?
    };
    protected String getMessage(WebApplicationException we) {
        StringBuilder mes = new StringBuilder();
        try {
            Response response = we.getResponse();
            response.bufferEntity();
            try {
                Error error = response.readEntity(Error.class);
                mes.append(error.toString());
            } catch (Exception e) {
                String m = response.readEntity(String.class);
                mes.append(response.getStatus()).append(':').append(m);
            }
            for (String s : HEADERS) {
                List<Object> v = response.getMetadata().get(s);
                if (v != null) {
                    mes.append("; ");
                    mes.append(s);
                    mes.append('=');
                    mes.append(v.stream().map(Objects::toString).collect(joining(",")));
                }
            }
        } catch (Exception e) {
            log.warn(e.getClass() + " " + e.getMessage());
            mes.append(we.getMessage());
        }

        return mes.toString();
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

