package nl.vpro.api.client.media;

import lombok.Getter;

import java.lang.reflect.Method;

import javax.ws.rs.core.Response;

import com.google.common.base.MoreObjects;

/**
 * @author Michiel Meeuwissen
 * @since 4.3.2
 */
@Getter
public class ResponseError extends RuntimeException {

    private final int status;
    private final Response.StatusType statusInfo;
    private final String entity;
    private final Method method;
    private final String description;

    public ResponseError(String description, Method method, int status, Response.StatusType statusType, String entity) {
        super(description + " " + method.getName() + " " + status + ":" + statusType + ":" + entity);
        this.status = status;
        this.statusInfo = statusType;
        this.entity = entity;
        this.method = method;
        this.description = description;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .omitNullValues()
            .add("status", status)
            .add("statusInfo", statusInfo)
            .add("entity", entity)
            .add("method", method)
            .add("description", description)
            .toString();
    }
}
