package nl.vpro.api.client.utils;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.api.rs.v3.profile.ProfileRestService;
import nl.vpro.domain.api.profile.Profile;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.api.profile.ProfileService;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.page.Page;

/**
 * @author Michiel Meeuwissen
 * @since 1.1
 */
@Named
public class ApiProfileServiceImpl implements ProfileService {

    final ProfileRestService client;

    @Inject
    public ApiProfileServiceImpl(NpoApiClients client) {
        this.client = client.getProfileService();
    }

    @Override
    public Profile getProfile(String name) {
        return client.load(name, null);
    }

    @Override
    public Profile getProfile(String name, Date on) {
        return client.load(name, on.getTime());
    }

    @Override
    public ProfileDefinition<Page> getPageProfileDefinition(String name) {
        return client.load(name, null).getPageProfile();
    }

    @Override
    public ProfileDefinition<MediaObject> getMediaProfileDefinition(String name) {
        return client.load(name, null).getMediaProfile();
    }

    @Override
    public ProfileDefinition<MediaObject> getMediaProfileDefinition(String name, Long since) {
        return client.load(name, since).getMediaProfile();
    }
}
