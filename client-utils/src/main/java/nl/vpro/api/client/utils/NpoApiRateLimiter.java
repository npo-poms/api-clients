package nl.vpro.api.client.utils;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Michiel Meeuwissen
 * @since 1.8
 */
@Named
public class NpoApiRateLimiter extends AbstractRateLimiter {

    @Inject
    @Override
    public void setBaseRate(@Named("npo-api.baserate") double baseRate) {
        super.setBaseRate(baseRate);
    }

    @Inject
    @Override
    public void setMinRate(@Named("npo-api.minrate") double minRate) {
        super.setMinRate(minRate);
    }
}
