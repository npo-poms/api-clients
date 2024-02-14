package nl.vpro.api.client.utils;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.*;

import nl.vpro.api.client.frontend.NpoApiClients;
import nl.vpro.api.rs.v3.profile.ProfileRestService;
import nl.vpro.domain.api.profile.*;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.page.Page;

/**
 * @author Michiel Meeuwissen
 * @since 1.1
 */
@Named
public class ApiProfileServiceImpl implements ProfileService {

    final NpoApiClients clients;

    // TODO arrange caching via ehcache (ehcache4guice or something)
    final LoadingCache<String, Profile> cache = CacheBuilder.newBuilder()
        .concurrencyLevel(4)
        .maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(
            new CacheLoader<>() {
                @Override
                public Profile load(@NotNull String profile) {
                    return getClient().load(profile);
                }
            });

    @Inject
    public ApiProfileServiceImpl(NpoApiClients clients) {
        this.clients = clients;
    }

    @Override
    public List<Profile> getProfiles() {
        return new ArrayList<>(getClient().list(".*", null).getItems());
    }

    @Override
    public Profile getProfile(String name) {
        try {
            return cache.get(name);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public ProfileDefinition<Page> getPageProfileDefinition(String name) {
        if (name == null) {
            return null;
        }

        Profile profile = getProfile(name);
        return profile == null ? null : profile.getPageProfile();
    }

    @Override
    public ProfileDefinition<MediaObject> getMediaProfileDefinition(String name) {
        if (name == null) {
            return null;
        }
        Profile profile = getProfile(name);
        return profile == null ? null : profile.getMediaProfile();
    }


    protected ProfileRestService getClient() {
        return clients.getProfileService();
    }

}
