package nl.vpro.api.client.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.UnmodifiableIterator;

import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.util.CloseableIterator;

/**
 * @author Michiel Meeuwissen
 * @since 1.0
 */
public class JsonArrayIterator<T> extends UnmodifiableIterator<T> implements CloseableIterator<T>, PeekingIterator<T> {

    final JsonParser jp;

    private T next = null;

    private final Class<T> clazz;

    private final Runnable callback;
    public JsonArrayIterator(InputStream inputStream, Class<T> clazz, Runnable callback) throws IOException {
        this.jp = Jackson2Mapper.getInstance().getFactory().createParser(inputStream);
        this.clazz = clazz;
        while(true) {
            JsonToken token = jp.nextToken();
            if (token == JsonToken.START_ARRAY) break;
        }
        jp.nextToken();
        this.callback = callback;
    }
    @Override
    public boolean hasNext() {
        findNext();
        return next != null;
    }

    @Override
    public T peek() {
        findNext();
        return next;
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
                if (next == null) {
                    if (callback != null) {
                        callback.run();
                    }
                }
            } catch (IOException e) {
                if (callback != null) {
                    callback.run();
                }
                throw new RuntimeException(e);
            }
        }
    }


    @Override
    public void close() throws IOException {
        if (callback != null) {
            callback.run();
        }
        this.jp.close();

    }

    public void write(OutputStream out) throws IOException {
        JsonGenerator jg = Jackson2Mapper.INSTANCE.getFactory().createGenerator(out);
        jg.writeStartObject();
        jg.writeArrayFieldStart("array");
        while (hasNext()) {
            T change = next();
            if (change != null) {
                jg.writeObject(change);
            } else {
                jg.writeNull();
            }
        }
        jg.writeEndArray();
        jg.writeEndObject();
        jg.flush();
    }
}
