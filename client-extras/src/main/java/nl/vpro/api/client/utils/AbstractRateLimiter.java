package nl.vpro.api.client.utils;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

/**
 * @author Michiel Meeuwissen
 * @since 1.1
 */
@SuppressWarnings("UnstableApiUsage")
public class AbstractRateLimiter {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    @Getter
    private double baseRate = 10000.0;
    @Getter
    private double minRate = 0.01;

    private final RateLimiter limiter = RateLimiter.create(baseRate);

    public void setBaseRate(double baseRate) {
        double prevFactor = limiter.getRate() / this.baseRate;
        this.baseRate = baseRate;
        limiter.setRate(this.baseRate * prevFactor);
    }

    public void setMinRate(double minRate) {
        this.minRate = minRate;
        setRate(limiter.getRate());
    }

    protected void acquire() {
        limiter.acquire();
    }

    protected void upRate() {
        setRate(limiter.getRate() * 2);
    }

    protected void downRate() {
        setRate(limiter.getRate() / 2);
    }

    private boolean setRate(double r) {
        if (r > baseRate) {
            r = baseRate;
        }
        if (r < minRate) {
            r = minRate;
        }
        boolean changed = r != limiter.getRate();
        if (changed) {
            LOG.info("Rate {} -> {} /s", limiter.getRate(), r);
            limiter.setRate(r);
        } else if (r == minRate) {
            LOG.debug("Rate {} / s", limiter.getRate());
        }
        return changed;
    }


}
