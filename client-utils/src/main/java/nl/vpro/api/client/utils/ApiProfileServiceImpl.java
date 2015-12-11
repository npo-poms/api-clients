package nl.vpro.api.client.utils;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

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

    // TODO arrange caching via ehcache (ehcache4guice or something)
    final LoadingCache<String, Profile> cache = CacheBuilder.newBuilder()
        .concurrencyLevel(4)
        .maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(
            new CacheLoader<String, Profile>() {
                @Override
                public Profile load(@NotNull String profile)  {
                    return client.load(profile, null);
                }
            });

    @Inject
    public ApiProfileServiceImpl(NpoApiClients client) {
        this.client = client.getProfileService();
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
	public Profile getProfile(String name, Instant on) {
        return client.load(name, on.toEpochMilli());
    }

    @Override
    public ProfileDefinition<Page> getPageProfileDefinition(String name) {
        return getProfile(name).getPageProfile();
    }

    @Override
    public ProfileDefinition<MediaObject> getMediaProfileDefinition(String name) {
        if (name == null) {
            return null;
        }
        return getProfile(name).getMediaProfile();
    }

    @Override
    public ProfileDefinition<MediaObject> getMediaProfileDefinition(String name, Long since) {
        return client.load(name, since).getMediaProfile();
    }
}
