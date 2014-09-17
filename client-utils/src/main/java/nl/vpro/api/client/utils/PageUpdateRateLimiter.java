package nl.vpro.api.client.utils;

import javax.inject.Named;

import com.google.inject.Inject;

/**
 * @author Michiel Meeuwissen
 * @since 1.1
 */
public class PageUpdateRateLimiter extends ClientRateLimiter {

    @Inject(optional = true)
    @Override
    public void setBaseRate(@Named("pageupdate-api.baserate") double baseRate) {
        super.setBaseRate(baseRate);
    }

    @Inject(optional = true)
    @Override
    public void setMinRate(@Named("pageupdate-api.minrate") double minRate) {
        super.setMinRate(minRate);
    }

}
