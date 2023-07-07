package nl.vpro.api.client.utils;

import java.io.InputStream;
import java.util.*;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.JAXB;

import nl.vpro.domain.api.profile.*;
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
            for (String res : resource.split("\\s*,\\s*")) {
                InputStream inputStream = getClass().getResourceAsStream(res);
                if (inputStream != null) {
                    Profile profile = JAXB.unmarshal(inputStream, Profile.class);
                    profiles.put(profile.getName(), profile);
                }
            }
        }
    }
    @Inject
    public XmlProfileServiceImpl(@Named("xmlprofileserviceimpl.resources") String resources) {
        this(new String[] {resources});
    }

    @Override
    public List<Profile> getProfiles() {
        return new ArrayList<>(profiles.values());
    }

    @Override
    public Profile getProfile(String name) {
        return profiles.get(name);
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

}
