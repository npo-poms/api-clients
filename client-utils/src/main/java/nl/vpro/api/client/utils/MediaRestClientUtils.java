package nl.vpro.api.client.utils;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vpro.api.rs.v3.media.MediaRestService;
import nl.vpro.domain.api.Match;
import nl.vpro.domain.api.MediaSearchResult;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.SearchResultItem;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.MediaFormBuilder;
import nl.vpro.domain.api.media.MediaSortField;
import nl.vpro.domain.api.media.MediaSortOrder;
import nl.vpro.domain.media.AVType;
import nl.vpro.domain.media.GroupType;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaType;
import nl.vpro.domain.media.ProgramType;
import nl.vpro.domain.media.SegmentType;

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


    public static MediaObject loadOrNull(MediaRestService restService, String urn) {
        try {
            return restService.load(urn, null);
        } catch (Exception e) {
            LOG.warn(e.getMessage());
            return null;
        }
    }

    public static MediaObject[] load(MediaRestService restService, String... ids) {
        // TODO MGNL-10958
        throw new UnsupportedOperationException();
    }

}
