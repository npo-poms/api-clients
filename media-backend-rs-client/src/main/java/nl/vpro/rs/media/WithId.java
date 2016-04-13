package nl.vpro.rs.media;

/**
 * @author Michiel Meeuwissen
 * @since 4.3
 */

import nl.vpro.domain.media.update.MediaUpdate;

public class WithId<T extends MediaUpdate> {

    final T update;
    final String id;

    public WithId(T update, String id) {
        this.update = update;
        this.id = id;
    }
}
