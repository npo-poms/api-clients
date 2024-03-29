package nl.vpro.api.client.utils;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import nl.vpro.domain.api.media.MediaRepository;

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

}
