package nl.vpro.api.client.utils;

import javax.inject.Named;

/**
 * @author Michiel Meeuwissen
 * @since 1.8
 */
@Named
public class NpoClientsRateLimiter extends ClientRateLimiter {

    @com.google.inject.Inject(optional = true)
    @Override
    public void setBaseRate(@Named("npo-api.baserate") double baseRate) {
        super.setBaseRate(baseRate);
    }

    @com.google.inject.Inject(optional = true)
    @Override
    public void setMinRate(@Named("npo-api.minrate") double minRate) {
        super.setMinRate(minRate);
    }
}
