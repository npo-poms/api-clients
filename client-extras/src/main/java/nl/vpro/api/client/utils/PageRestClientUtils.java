package nl.vpro.api.client.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

import nl.vpro.api.rs.v3.page.PageRestService;
import nl.vpro.domain.api.page.PageForm;
import nl.vpro.domain.page.Page;
import nl.vpro.jackson2.JsonArrayIterator;
import nl.vpro.util.*;

/**
 * @author Michiel Meeuwissen
 * @since 5.6
 */
@Slf4j
public class PageRestClientUtils {


    public static CloseableIterator<Page> iterate(PageRestService restService, PageForm form, String profile) {

        PageForm f = form == null ? new PageForm() : form;
        return new LazyIterator<>(() -> {
            try {

                final InputStream inputStream = restService.iterate(f, profile, null, 0L, Integer.MAX_VALUE).readEntity(InputStream.class);
                // Cache the stream to a file first.
                // If we don't do this, the stream seems to be inadvertedly truncated sometimes if the client doesn't consume the iterator fast enough.
                FileCachingInputStream cacheToFile = FileCachingInputStream.builder()
                    .filePrefix("iterate-" + profile + "-")
                    .batchConsumer((t, c) -> log.debug("Creating {} ({} bytes written)", t.getTempFile(), c.getCount()))
                    .batchSize(5000000L)
                    .logger(log)
                    .input(inputStream)
                    .build();
                return JsonArrayIterator.<Page>builder()
                    .inputStream(cacheToFile)
                    .valueClass(Page.class)
                    .callback(() -> MediaRestClientUtils.closeQuietly(inputStream, cacheToFile))
                    .logger(log)
                    .build();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }



}
