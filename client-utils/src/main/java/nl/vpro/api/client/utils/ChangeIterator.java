package nl.vpro.api.client.utils;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.collect.UnmodifiableIterator;

import nl.vpro.domain.api.Change;
import nl.vpro.jackson2.Jackson2Mapper;

/**
 * @author Michiel Meeuwissen
 * @since 1.0
 */
public class ChangeIterator extends UnmodifiableIterator<Change> {

    final JsonParser jp;

    private Change next = null;

    public ChangeIterator(InputStream inputStream) throws IOException {
        this.jp = Jackson2Mapper.getInstance().getFactory().createParser(inputStream);
        while(true) {
            JsonToken token = jp.nextToken();
            if (token == JsonToken.START_ARRAY) break;
        }
        jp.nextToken();
    }
    @Override
    public boolean hasNext() {
        findNext();
        return next != null;
    }

    @Override
    public Change next() {
        findNext();
        Change result = next;
        next = null;
        return result;
    }
    protected void findNext() {
        if (next == null) {
            try {
                next = jp.readValueAs(Change.class);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
