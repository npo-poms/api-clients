package nl.vpro.api.client.utils;

import javax.inject.Inject;
import javax.inject.Named;

import nl.vpro.domain.api.media.MediaRepository;
import nl.vpro.domain.media.MediaObject;

/**
 * @author Michiel Meeuwissen
 * @since 1.1
 */
@Named("mediaLoadRepository")
public class ApiClientMediaRepository extends AbstractApiClientMediaRepository implements MediaRepository {

    @Inject
    public ApiClientMediaRepository(NpoApiMediaUtil util) {
        super(util);
    }


    @Override
    public <T extends MediaObject> T findByMid(String mid, boolean loadDeleted) {
        return null;

    }
}
