package nl.vpro.api.client.utils;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import nl.vpro.domain.api.media.*;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.media.MediaObject;

/**
 * An implementation of {@link MediaSearchRepository}, but based on an api client.
 *
 * @author Michiel Meeuwissen
 * @since 1.1
 */
@Named("npoApiMediaSearchRepository")
public class ApiClientMediaSearchRepository extends AbstractApiClientMediaRepository implements MediaSearchRepository {

    @Inject
    public ApiClientMediaSearchRepository(NpoApiMediaUtil util) {
        super(util);
    }

    @Override
    public MediaSearchResult find(ProfileDefinition<MediaObject> profile, MediaForm form, long offset, Integer max) {
        return clients.getMediaService().find(form != null ? form : MediaFormBuilder.emptyForm(), name(profile), null, offset, max);

    }

    @Override
    public MediaSearchResult findMembers(MediaObject media, ProfileDefinition<MediaObject> profile, MediaForm form, long offset, Integer max) {
        return clients.getMediaService().findMembers(form != null ? form : MediaFormBuilder.emptyForm(), media.getMid(), name(profile), null, offset, max);

    }

    @Override
    public ProgramSearchResult findEpisodes(MediaObject media, ProfileDefinition<MediaObject> profile, MediaForm form, long offset, Integer max) {
        return clients.getMediaService().findEpisodes(form != null ? form : MediaFormBuilder.emptyForm(), media.getMid(), name(profile), null, offset, max);
    }

    @Override
    public MediaSearchResult findDescendants(MediaObject media, ProfileDefinition<MediaObject> profile, MediaForm form, long offset, Integer max) {
        return clients.getMediaService().findDescendants(form != null ? form : MediaFormBuilder.emptyForm(), media.getMid(), name(profile), null, offset, max);
    }

    @Override
    public MediaSearchResult findRelated(MediaObject media, ProfileDefinition<MediaObject> profile, MediaForm form, Integer max) {
        return clients.getMediaService().findRelated(form != null ? form : MediaFormBuilder.emptyForm(), media.getMid(), name(profile), null, max, null);
    }


}
