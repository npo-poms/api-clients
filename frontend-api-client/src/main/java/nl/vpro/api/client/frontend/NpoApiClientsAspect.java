package nl.vpro.api.client.frontend;

import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;

import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import static nl.vpro.domain.api.Constants.*;

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
            fillImplicitParameters(method, args);
            Object invoke = method.invoke(proxied, args);
            dealWithHeaders(method, args);
            return invoke;
        } catch (InvocationTargetException itc) {
            throw itc.getCause();
        } catch(WebApplicationException ise) {
            log.error(ise.getResponse().getEntity().toString(), ise);
            throw ise;
        }
    }

    protected void fillImplicitParameters(Method method, Object[] args) {
        Annotation[][] annotations = method.getParameterAnnotations();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                for (int j = 0; j < annotations[i].length; j++) {
                    if (annotations[i][j] instanceof QueryParam && args[i] == null) {
                        QueryParam queryParam = (QueryParam) annotations[i][j];
                        if (args[i] == null) {
                            if (PROPERTIES.equals(queryParam.value())) {
                                log.debug("Implicitly set properties parameter to {}", clients.getProperties());
                                args[i] = clients.getProperties();
                            } else if (PROFILE.equals(queryParam.value())) {
                                log.debug("Implicitly set profile parameter to {}", clients.getProfile());
                                args[i] = clients.getProfile();
                            } else if (MAX.equals(queryParam.value())) {
                                log.debug("Implicitly set max parameter to {}", clients.getMax());
                                args[i] = clients.getMax();
                            }
                        }
                    }
                }

            }
        }
    }

    protected void dealWithHeaders(Method method, Object[] args) {
        clients.dealWithHeaders((k) ->  clients.getHeaderLevel().apply(method, args, k));
    }



}
