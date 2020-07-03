package nl.vpro.api.client.media;

import nl.vpro.domain.media.update.MediaUpdate;


/**
 * @author Michiel Meeuwissen
 * @since 4.3
 */
public class WithId<T extends MediaUpdate<?>> {

    final T update;
    final String id;

    public WithId(T update, String id) {
        this.update = update;
        this.id = id;
    }
}
