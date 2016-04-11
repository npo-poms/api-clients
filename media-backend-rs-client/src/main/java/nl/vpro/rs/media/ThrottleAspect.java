package nl.vpro.rs.media;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServiceUnavailableException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michiel Meeuwissen
 * @since 4.3
 */
class ThrottleAspect implements InvocationHandler {

    private static Logger LOG = LoggerFactory.getLogger(ThrottleAspect.class);

    private final MediaRestClient client;
    private final MediaRestController proxied;

    ThrottleAspect(MediaRestClient client, MediaRestController proxied) {
        this.client = client;
        this.proxied = proxied;
    }

    public static MediaRestController proxy(MediaRestClient client, MediaRestController restController) {
        return (MediaRestController) Proxy.newProxyInstance(MediaRestClient.class.getClassLoader(), 
            new Class[]{MediaRestController.class}, new ThrottleAspect(client, restController));
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        while (true) {
            LOG.debug("Throttling {} (rate: {})", method, client.getThrottleRate());
            client.throttle();
            try {
                try {
                    return method.invoke(proxied, args);
                } catch (InvocationTargetException itc) {
                    throw itc.getCause();
                }
            } catch (NotFoundException nfe) {
                return null;
            } catch (ServiceUnavailableException sue) {
                client.retryAfterWaitOrException(method.getName() + ": Service unavailable");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
