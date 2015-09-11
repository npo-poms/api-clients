package nl.vpro.api.client.utils;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXB;

import nl.vpro.domain.api.profile.Profile;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.api.profile.ProfileService;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.page.Page;

/**
 * @author Michiel Meeuwissen
 * @since 1.17
 */
public class XmlProfileServiceImpl implements ProfileService {

    private final Map<String, Profile> profiles = new HashMap<>();

    public XmlProfileServiceImpl(InputStream... inputStreams) {
        for (InputStream inputStream : inputStreams) {
            if (inputStream != null) {
                Profile profile = JAXB.unmarshal(inputStream, Profile.class);
                profiles.put(profile.getName(), profile);
            }
        }
    }

    public XmlProfileServiceImpl(String... resources) {
        for (String resource : resources) {
            InputStream inputStream = getClass().getResourceAsStream(resource);
            if (inputStream != null) {
                Profile profile = JAXB.unmarshal(inputStream, Profile.class);
                profiles.put(profile.getName(), profile);
            }
        }
    }

    @Override
    public Profile getProfile(String name) {
        return profiles.get(name);

    }

    @Override
    public Profile getProfile(String name, Date on) {
        return getProfile(name);

    }

    @Override
    public ProfileDefinition<Page> getPageProfileDefinition(String name) {
        Profile profile = getProfile(name);
        return profile == null ? null : profile.getPageProfile();

    }

    @Override
    public ProfileDefinition<MediaObject> getMediaProfileDefinition(String name) {
        Profile profile = getProfile(name);
        return profile == null ? null : profile.getMediaProfile();

    }

    @Override
    public ProfileDefinition<MediaObject> getMediaProfileDefinition(String name, Long since) {
        return getMediaProfileDefinition(name);
    }
}
