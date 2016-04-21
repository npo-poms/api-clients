package nl.vpro.rs.media;

import javax.ws.rs.core.Response;

/**
 * @author Michiel Meeuwissen
 * @since 4.3.2
 */
public class ResponseError extends RuntimeException {

    private final int status;
    private final Response.StatusType statusInfo;
    private final String entity;

    public ResponseError(int status, Response.StatusType statusType, String entity) {
        super(status + ":" + statusType + ":" + entity);
        this.status = status;
        this.statusInfo = statusType;
        this.entity = entity;
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


}
