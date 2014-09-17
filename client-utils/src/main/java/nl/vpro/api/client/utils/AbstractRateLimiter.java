package nl.vpro.api.client.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

/**
 * @author Michiel Meeuwissen
 * @since 1.1
 */
public class AbstractRateLimiter {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());


    private double baseRate = 10000.0;
    private double minRate = 0.01;


    private final RateLimiter limiter = RateLimiter.create(baseRate);

    public double getBaseRate() {
        return baseRate;
    }

    public void setBaseRate(double baseRate) {
        double prevFactor = limiter.getRate() / this.baseRate;
        this.baseRate = baseRate;
        limiter.setRate(this.baseRate * prevFactor);
    }

    public double getMinRate() {
        return minRate;

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

    private void setRate(double r) {
        if (r > baseRate) {
            r = baseRate;
        }
        if (r < minRate) {
            r = minRate;
        }
        if (r != limiter.getRate()) {
            LOG.info("Rate " + limiter.getRate() + " -> " + r);
            limiter.setRate(r);
        }
    }


}
