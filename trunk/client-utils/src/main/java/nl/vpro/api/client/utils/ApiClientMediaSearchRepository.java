package nl.vpro.api.client.utils;

import javax.inject.Inject;
import javax.inject.Named;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.api.MediaSearchResult;
import nl.vpro.domain.api.ProgramSearchResult;
import nl.vpro.domain.api.ScheduleSearchResult;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.MediaFormBuilder;
import nl.vpro.domain.api.media.MediaSearchRepository;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.media.MediaObject;

/**
 * @author Michiel Meeuwissen
 * @since 1.1
 */
@Named
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
    public ScheduleSearchResult findSchedules(ProfileDefinition<MediaObject> profile, MediaForm form, Long offset, Integer max) {
        throw new UnsupportedOperationException();
    }
}