package nl.vpro.api.client.utils;

import lombok.Getter;
import lombok.With;

import java.io.Serializable;
import java.time.Instant;

import org.checkerframework.checker.nullness.qual.Nullable;

import nl.vpro.domain.api.*;

/**
 * @since 7.2
 */
@Getter
public class ChangesFeedParameters implements Serializable {

    final @Nullable String profile;

    @With
    final MediaSince mediaSince;
    final Order order;

    final Integer max;
    final Deletes deletes;
    final Tail tail;
    final String reasonFilter;


    public static ChangesFeedParameters.Builder changesParameters() {
        return builder();
    }

    @lombok.Builder(builderClassName = "Builder", buildMethodName = "_build")
    private ChangesFeedParameters(
            @Nullable String profile,
            MediaSince mediaSince, Order order, Integer max, Deletes deletes, Tail tail, String reasonFilter) {
        this.profile = profile;
        this.mediaSince = mediaSince;
        this.order = order == null ? Order.ASC : order;
        this.max = max;
        this.deletes = deletes == null ? Deletes.ID_ONLY : deletes;
        this.tail = tail == null ? Tail.IF_EMPTY : tail;
        this.reasonFilter = reasonFilter;
    }

    public static class Builder {

        Instant since;
        String mid;

        public Builder since(Instant since) {
            this.since = since;
            return this;
        }

        public Builder mid(String mid) {
            this.mid = mid;
            return this;
        }

        public ChangesFeedParameters build() {
            if (since != null || mid != null) {
                mediaSince(MediaSince.builder().instant(since).mid(mid).build());
            }
            return _build();
        }

    }
}
