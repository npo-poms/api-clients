package nl.vpro.api.client.media;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

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
                if (context.getEntity() instanceof InputStream inputStream) {
                    IOUtils.copy(inputStream, context.getOutputStream());
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
