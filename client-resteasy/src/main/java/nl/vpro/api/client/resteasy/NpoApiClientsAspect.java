package nl.vpro.api.client.resteasy;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * This Proxy:
 * - throttles all calls
 * - automaticly fills some common arguments (recognized by @QueryParam annotations)
 * - if the return type is Response, it also checks the status code
 * @author Michiel Meeuwissen
 * @since 4.9
 */
@Slf4j
class NpoApiClientsAspect<T> implements InvocationHandler {

    private final T proxied;
    private final NpoApiClients clients;

    NpoApiClientsAspect(NpoApiClients clients, T proxied) {
        this.proxied = proxied;
        this.clients = clients;
    }

    public static <T, S> T proxy(NpoApiClients clients, T proxied, Class<S> service) {
        return (T) Proxy.newProxyInstance(NpoApiClientsAspect.class.getClassLoader(),
            new Class[]{service}, new NpoApiClientsAspect<T>(clients, proxied));
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            fillPropertiesParameter(method, args);
            return method.invoke(proxied, args);
        } catch (InvocationTargetException itc) {
            throw itc.getCause();
        }
    }

    protected void fillPropertiesParameter(Method method, Object[] args) {
        Annotation[][] annotations = method.getParameterAnnotations();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                for (int j = 0; j < annotations[i].length; j++) {
                    if (annotations[i][j] instanceof QueryParam && args[i] == null) {
                        QueryParam queryParam = (QueryParam) annotations[i][j];
                        if ("properties".equals(queryParam.value())) {
                            log.debug("Implicetely set errors parameter to {}", clients.getProperties());
                            args[i] = clients.getProperties();
                        }
                    }
                }

            }
        }

    }
}
