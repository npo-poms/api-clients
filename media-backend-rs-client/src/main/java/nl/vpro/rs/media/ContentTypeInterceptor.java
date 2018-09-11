package nl.vpro.rs.media;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.commons.io.IOUtils;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Slf4j
public class ContentTypeInterceptor implements WriterInterceptor {
    public static final ThreadLocal<String> CONTENT_TYPE = ThreadLocal.withInitial(() -> null);
    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        if (CONTENT_TYPE.get() != null) {
            try {
                context.getHeaders().put(HttpHeaders.CONTENT_TYPE, Collections.singletonList(CONTENT_TYPE.get()));
                if (context.getEntity() instanceof InputStream) {
                    IOUtils.copy((InputStream) context.getEntity(), context.getOutputStream());
                } else {
                    context.proceed();
                }
            } finally {
                CONTENT_TYPE.remove();
            }
        } else {
            context.proceed();
        }

    }
}
