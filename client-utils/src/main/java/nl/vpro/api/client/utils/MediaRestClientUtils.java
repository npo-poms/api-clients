package nl.vpro.api.client.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.inject.Named;
import javax.ws.rs.ProcessingException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.MediaFormBuilder;
import nl.vpro.domain.api.media.MediaSortField;
import nl.vpro.domain.api.media.MediaSortOrder;
import nl.vpro.domain.media.*;

/**
 * @author Michiel Meeuwissen
 * @since 1.1
 */
public class MediaRestClientUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MediaRestClientUtils.class);

    /**
     * Similar to the v1 call for easier migration
     */
    public static MediaForm getRecentPlayablePrograms(AVType avType) {

        MediaFormBuilder formBuilder = MediaFormBuilder.form();

        if (avType != null) {
            formBuilder.avTypes(Match.MUST, avType);
        }

        List<MediaType> excludedTypes = new ArrayList<>();
        for (GroupType type : GroupType.values()) {
            excludedTypes.add(type.getMediaType());
        }
        excludedTypes.add(ProgramType.TRACK.getMediaType());
        excludedTypes.add(SegmentType.SEGMENT.getMediaType());

        MediaForm form = formBuilder.types(Match.NOT, excludedTypes.toArray(new MediaType[excludedTypes.size()])).build();
        form.addSortField(new MediaSortOrder(MediaSortField.sortDate, Order.DESC));

        return form;
    }

    public static List<MediaObject> adapt(final MediaSearchResult result) {
        return new AbstractList<MediaObject>() {

            @Override
            public MediaObject get(int index) {
                SearchResultItem<? extends MediaObject> object = result.getItems().get(index);
                return object.getResult();

            }

            @Override
            public int size() {
                return result.getSize();
            }
        };
    }

    public static MediaObject loadOrNull(MediaRestService restService, String id) {
        try {
            return restService.load(id, null);
        } catch (Exception e) {
            LOG.warn(e.getMessage());
            return null;
        }
    }

    public static MediaObject[] load(MediaRestService restService, String... ids) {
        return loadWithLoadAll(restService, ids);
        //loadWithSearch(restService, ids); // doesn't preserve order/duplicates, probable slower too.
    }

    private static MediaObject[] loadWithLoadAll(MediaRestService restService, String... ids) {
        List<MediaObject> result = new ArrayList<>(ids.length);
        for (List<String> idList : Lists.partition(Arrays.asList(ids), 500)) {
            MediaResult mediaResult = restService.list(null, null, 0l, ids.length, StringUtils.join(idList, ","));
            result.addAll(mediaResult.getItems());
        }
        return result.toArray(new MediaObject[result.size()]);
    }
    // unused for now
    private static MediaObject[] loadWithSearch(MediaRestService restService, String... ids) {
        List<MediaObject> result = new ArrayList<>(ids.length);

        /*
         * Calling restService.find with max > Constants.MAX_RESULTS gets you a BadRequestException,
         * so the list of ids is partitioned first and if needed multiple find calls are executed...
         */
        for (List<String> idList : Lists.partition(Arrays.asList(ids), Constants.MAX_RESULTS)) {
            String[] partitionedIds = idList.toArray(new String[idList.size()]);
            MediaForm mediaForm = MediaFormBuilder.form().mediaIds(partitionedIds).build();
            MediaSearchResult mediaSearchResult = restService.find(mediaForm, null, null, 0L, idList.size());
            result.addAll(adapt(mediaSearchResult));
        }

        return result.toArray(new MediaObject[result.size()]);
    }

    public static CloseableIterator<Change> changes(MediaRestService restService, String profile, long since, Order order, Integer max) throws IOException {
        try {
            final InputStream inputStream = restService.changes(profile, null, since, order.name().toLowerCase(), max, null, null);
            return new JsonArrayIterator<>(inputStream, Change.class, new Runnable() {
                @Override
                public void run() {
                    IOUtils.closeQuietly(inputStream);
                }
            });
        } catch (ProcessingException pi) {
            Throwable t = pi.getCause();
            throw new RuntimeException(t.getMessage(), t);
        }

    }

    /**
     *
     * @deprecated We'll make a sitemap feature on page rest service.
     */
    @Deprecated
    public static Iterator<MediaObject> iterate(MediaRestService restService, MediaForm form, String profile) throws IOException {
        final InputStream inputStream = restService.iterate(form, profile, null, 0l, Integer.MAX_VALUE, null, null);
        return new JsonArrayIterator<>(inputStream, MediaObject.class, new Runnable() {
            @Override
            public void run() {
                IOUtils.closeQuietly(inputStream);
            }
        });

    }

    /**
     * Only call this during the migration to NPO API while not everything is converted to MID yet.
     *
     * @deprecated Migrate code and data from URN to MID.
     */
    @Deprecated
    public static String toMid(MediaRestService restService, String urn) {
        MediaForm mediaForm = MediaFormBuilder.form().mediaIds(urn).build();
        MediaSearchResult result = restService.find(mediaForm, null, "mid", 0L, 1);
        if (result.getSize() == 0) {
            return null;
        }
        return result.getItems().get(0).getResult().getMid();
    }
}
