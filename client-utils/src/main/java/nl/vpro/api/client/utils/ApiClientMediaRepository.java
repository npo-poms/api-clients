package nl.vpro.api.client.utils;

import javax.inject.Inject;
import javax.inject.Named;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.api.media.MediaRepository;

/**
 * @author Michiel Meeuwissen
 * @since 1.1
 */
@Named
public class ApiClientMediaRepository extends AbstractApiClientMediaRepository implements MediaRepository {

    @Inject
    public ApiClientMediaRepository(NpoApiMediaUtil util) {
        super(util);
    }
}
