package nl.vpro.rs.media;

import java.lang.reflect.Method;

import javax.ws.rs.core.Response;

/**
 * @author Michiel Meeuwissen
 * @since 4.3.2
 */
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

    public int getStatus() {
        return status;
    }

    public Response.StatusType getStatusInfo() {
        return statusInfo;
    }

    public String getEntity() {
        return entity;
    }


    public Method getMethod() {
        return method;
    }

    public String getDescription() {
        return description;
    }
}
