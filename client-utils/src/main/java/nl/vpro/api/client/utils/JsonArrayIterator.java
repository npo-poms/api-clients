package nl.vpro.api.client.utils;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.collect.UnmodifiableIterator;

import nl.vpro.jackson2.Jackson2Mapper;

/**
 * @author Michiel Meeuwissen
 * @since 1.0
 */
public class JsonArrayIterator<T> extends UnmodifiableIterator<T> {

    final JsonParser jp;

    private T next = null;

    private final Class<T> clazz;

    public JsonArrayIterator(InputStream inputStream, Class<T> clazz) throws IOException {
        this.jp = Jackson2Mapper.getInstance().getFactory().createParser(inputStream);
        this.clazz = clazz;
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
    public T next() {
        findNext();
        T result = next;
        next = null;
        return result;
    }
    protected void findNext() {
        if (next == null) {
            try {
                next = jp.readValueAs(clazz);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
