package nl.vpro.api.client.utils;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.context.annotation.Primary;

import nl.vpro.domain.api.media.*;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.media.MediaObject;

/**
 * @author Michiel Meeuwissen
 * @since 1.1
 */
@Named("npoApiMediaSearchRepository")
@Primary
public class ApiClientMediaSearchRepository extends AbstractApiClientMediaRepository implements MediaSearchRepository {

    @Inject
    public ApiClientMediaSearchRepository(NpoApiMediaUtil util) {
        super(util);
    }


    @Override
    public MediaSearchResult find(ProfileDefinition<MediaObject> profile, MediaForm form, Long offset, Integer max) {
        return clients.getMediaService().find(form != null ? form : MediaFormBuilder.emptyForm(), profile != null ? profile.getName() : null, null, offset, max);

    }

    @Override
    public MediaSearchResult findMembers(MediaObject media, ProfileDefinition<MediaObject> profile, MediaForm form, Long offset, Integer max) {
        return clients.getMediaService().findMembers(form != null ? form : MediaFormBuilder.emptyForm(), media.getMid(), profile != null ? profile.getName() : null, null, offset, max);

    }

    @Override
    public ProgramSearchResult findEpisodes(MediaObject media, ProfileDefinition<MediaObject> profile, MediaForm form, Long offset, Integer max) {
        return clients.getMediaService().findEpisodes(form != null ? form : MediaFormBuilder.emptyForm(), media.getMid(), profile != null ? profile.getName() : null, null, offset, max);
    }

    @Override
    public MediaSearchResult findDescendants(MediaObject media, ProfileDefinition<MediaObject> profile, MediaForm form, Long offset, Integer max) {
        return clients.getMediaService().findDescendants(form != null ? form : MediaFormBuilder.emptyForm(), media.getMid(), profile != null ? profile.getName() : null, null, offset, max);
    }

    @Override
    public MediaSearchResult findRelated(MediaObject media, ProfileDefinition<MediaObject> profile, MediaForm form, Integer max) {
        return clients.getMediaService().findRelated(form != null ? form : MediaFormBuilder.emptyForm(), media.getMid(), profile != null ? profile.getName() : null, null, max);
    }

    @Override
    public MediaSearchResult findRelatedInTopspin(MediaObject media, ProfileDefinition<MediaObject> profile, MediaForm form, Integer max) {

        // TODO, this should of course go to topspin!
        return findRelated(media, profile, form, max);

    }


}
