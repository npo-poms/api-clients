package nl.vpro.api.client.utils;

import lombok.Getter;

import jakarta.ws.rs.core.Response;

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

    @lombok.Builder
    private Result(Status status, String errors, Throwable cause, E entity) {
        this.status = status;
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


    public static <E> Result<E> notneeded() {
        return new Result<>(Status.NOTNEEDED, null);
    }

    public static <E> Result<E> error(String message) {
        return new Result<>(Status.ERROR, message);
    }

    public static <E> Result<E> fatal(String message, Throwable t) {
        return new Result<>(Status.FATAL_ERROR, message, t, null);
    }

    public static <E> Result<E> notfound(String message) {
        return new Result<>(Status.NOTFOUND, message);
    }

    public static <E> Result<E> aborted(String message) {
        return new Result<>(Status.ABORTED, message);
    }

    public static <E> Result<E> denied(String message) {
        return new Result<>(Status.DENIED, message);
    }

    public static <E> Result<E> invalid(String message) {
        return new Result<>(Status.INVALID, message);
    }

    public boolean needsRetry() {
        return status != null && status.needsRetry;
    }

    public boolean isOk() {
        return status != null && status.ok;
    }


    @Override
    public String toString() {
        return status + (errors != null ? (":" + errors) : entity == null ? "" : " " + entity);
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
