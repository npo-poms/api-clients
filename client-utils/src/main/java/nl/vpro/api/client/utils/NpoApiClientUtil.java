package nl.vpro.api.client.utils;

import java.io.IOException;
import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;

import nl.vpro.api.client.resteasy.NpoApiClients;
import nl.vpro.domain.api.Change;
import nl.vpro.domain.api.Order;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaType;

/**
 * @author Michiel Meeuwissen
 */
public class NpoApiClientUtil extends AbstractClientUtil {


    final NpoApiClients clients;


    @Inject
    public NpoApiClientUtil(NpoApiClients clients) {
        this.clients = clients;
    }

    @com.google.inject.Inject(optional = true)
    @Override
    public void setBaseRate(@Named("npo-api.clientutil.baserate") double baseRate) {
        super.setBaseRate(baseRate);
    }

    @com.google.inject.Inject(optional = true)
    @Override
    public void setMinRate(@Named("npo-api.clientutil.minrate") double minRate) {
        super.setMinRate(minRate);
    }

    public MediaObject loadOrNull(String id) {
        acquire();
        try {
            MediaObject object = MediaRestClientUtils.loadOrNull(clients.getMediaService(), id);
            upRate();
            return object;
        } catch (RuntimeException rte) {
            downRate();
            throw rte;
        }
    }

    public MediaObject[] load(String... ids) {
        acquire();
        try {
            MediaObject[] result = MediaRestClientUtils.load(clients.getMediaService(), ids);
            upRate();
            return result;
        } catch (RuntimeException rte) {
            downRate();
            throw rte;
        }
    }

    public Iterator<Change> changes(String profile, long since, Order order, Integer max) {
        acquire();
        try {
            Iterator<Change> result = MediaRestClientUtils.changes(clients.getMediaService(), profile, since, order, max);
            upRate();
            return result;
        } catch (IOException e) {
            downRate();
            throw new RuntimeException(clients + ":" + e.getMessage(), e);
        }
    }

    public ProfileDefinition<MediaObject> getMediaProfile(String profile) {
        acquire();
        return clients.getProfileService().load(profile, null).getMediaProfile();
    }

    @Deprecated
    public Iterator<MediaObject> iterate(MediaForm form, String profile)  {
        acquire();
        try {
            Iterator<MediaObject> result = MediaRestClientUtils.iterate(clients.getMediaService(), form, profile);
            upRate();
            return result;
        } catch (Throwable e) {
            downRate();
            throw new RuntimeException(clients + ":" + e.getMessage(), e);
        }
    }

    @Deprecated
    public String toMid(String urn) {
        return MediaRestClientUtils.toMid(clients.getMediaService(), urn);
    }

    public MediaType getType(String mid) {
        return load(mid)[0].getMediaType();
    }

    @Override
    public String toString() {
        return String.valueOf(clients);
    }


}
