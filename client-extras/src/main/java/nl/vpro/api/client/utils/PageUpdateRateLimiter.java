package nl.vpro.api.client.utils;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Michiel Meeuwissen
 * @since 1.1
 */
@lombok.Builder
public class PageUpdateRateLimiter extends AbstractRateLimiter {

    @Inject
    @Override
    public void setBaseRate(@Named("npo_pageupdate_api.baserate") double baseRate) {
        super.setBaseRate(baseRate);
    }

    @Inject
    @Override
    public void setMinRate(@Named("npo_pageupdate_api.minrate") double minRate) {
        super.setMinRate(minRate);
    }

}
