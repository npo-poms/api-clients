package nl.vpro.api.client.utils;

import com.google.common.collect.Lists;
import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.MediaFormBuilder;
import nl.vpro.domain.api.media.MediaSortField;
import nl.vpro.domain.api.media.MediaSortOrder;
import nl.vpro.domain.media.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Michiel Meeuwissen
 * @since 2.3
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

    public static String toMid(MediaRestService restService, String urn) {
        MediaForm mediaForm = MediaFormBuilder.form().mediaIds(urn).build();
        MediaSearchResult result = restService.find(mediaForm, null, "mid", 0L, 1);
        if (result.getSize() == 0) {
            return null;
        }
        return result.getItems().get(0).getResult().getMid();
    }
}