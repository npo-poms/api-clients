package nl.vpro.api.client.utils;

import lombok.Getter;

import javax.ws.rs.core.Response;

/**
 * @author Michiel Meeuwissen
 * @since 1.0
 */
public class Result<E> {

    @Getter
    private final Status status;

    @Getter
    private final String errors;

    @Getter
    private final Throwable cause;

    @Getter
    private final E entity;


    private Result(Status success, String errors) {
        this(success, errors, null, null);
    }

    private Result(Status success, String errors, Throwable cause, E entity) {
        this.status = success;
        this.errors = errors;
        this.cause = cause;
        this.entity = entity;
    }

    public static Result<Void> success() {
        return new Result<>(Status.SUCCESS, null);
    }

    public static <E> Result<E> success(E entity) {
        return new Result<>(Status.SUCCESS, null, null, entity);
    }

    public static <E> Result<E> success(Response response, Class<E> entityClass) {
        if (Void.class.equals(entityClass)) {
            return success(null);
        } else {
            return success(response.readEntity(entityClass));
        }
    }


    public static Result<Void> notneeded() {
        return new Result<>(Status.NOTNEEDED, null);
    }

    public static Result<Void> error(String message) {
        return new Result<>(Status.ERROR, message);
    }

    public static Result<Void> fatal(String message, Throwable t) {
        return new Result<>(Status.FATAL_ERROR, message, t, null);
    }

    public static Result<Void> notfound(String message) {
        return new Result<>(Status.NOTFOUND, message);
    }

    public static Result<Void> aborted(String message) {
        return new Result<>(Status.ABORTED, message);
    }

    public static Result<Void> denied(String message) {
        return new Result<>(Status.DENIED, message);
    }

    public static Result<Void> invalid(String message) {
        return new Result<>(Status.INVALID, message);
    }

    public boolean needsRetry() {
        return status.needsRetry;
    }

    public boolean isOk() {
        return status.ok;
    }


    @Override
    public String toString() {
        return status + (errors != null ? (":" + errors) : "");
    }

    public enum Status {
        //  ok
        SUCCESS(false, true),
        NOTNEEDED(false, true),

        // retryables errors
        NOTFOUND(true, false),
        ERROR(true, false),
        ABORTED(true, false),

        // non retryables errors
        DENIED(false, false),
        INVALID(false, false),
        FATAL_ERROR(false, false)
        ;

        private final boolean needsRetry;
        private final boolean ok;

        Status(boolean needsRetry, boolean ok) {
            this.needsRetry = needsRetry;
            this.ok = ok;
        }
    }
}
