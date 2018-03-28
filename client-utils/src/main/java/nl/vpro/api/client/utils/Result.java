package nl.vpro.api.client.utils;

import lombok.Getter;

/**
 * @author Michiel Meeuwissen
 * @since 1.0
 */
public class Result {

    @Getter
    private final Status status;

    @Getter
    private final String errors;

    @Getter
    private final Throwable cause;

    private Result(Status success, String errors) {
        this(success, errors, null);
    }

     private Result(Status success, String errors, Throwable cause) {
        this.status = success;
        this.errors = errors;
        this.cause = cause;
    }

    public static Result success() {
        return new Result(Status.SUCCESS, null);
    }

    public static Result notneeded() {
        return new Result(Status.NOTNEEDED, null);
    }

    public static Result error(String message) {
        return new Result(Status.ERROR, message);
    }

    public static Result fatal(String message, Throwable t) {
        return new Result(Status.FATAL_ERROR, message, t);
    }

    public static Result notfound(String message) {
        return new Result(Status.NOTFOUND, message);
    }

    public static Result aborted(String message) {
        return new Result(Status.ABORTED, message);
    }

    public static Result denied(String message) {
        return new Result(Status.DENIED, message);
    }

    public static Result invalid(String message) {
        return new Result(Status.INVALID, message);
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
