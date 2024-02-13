package nl.vpro.api.client.utils;

import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * @author Michiel Meeuwissen
 * @since 1.1
 */
@lombok.Builder
public class PageUpdateRateLimiter extends AbstractRateLimiter {

    @Inject
    @Override
    public void setBaseRate(@Named("npo-pages_publisher.baserate") double baseRate) {
        super.setBaseRate(baseRate);
    }

    @Inject
    @Override
    public void setMinRate(@Named("npo-pages_publisher.minrate") double minRate) {
        super.setMinRate(minRate);
    }

}
