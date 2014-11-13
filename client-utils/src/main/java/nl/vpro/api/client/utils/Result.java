package nl.vpro.api.client.utils;

/**
 * @author Michiel Meeuwissen
 * @since 1.0
 */
public class Result {

    private final Status status;

    private final String errors;

    private Result(Status success, String errors) {
        this.status = success;
        this.errors = errors;
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

    public boolean isSuccess() {
        return status != Status.ERROR && status != Status.ABORTED && status != Status.NOTFOUND;
    }
    public Status getStatus() {
        return status;
    }

    public String getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return status + (errors != null ? (":" + errors) : "");
    }

    public enum Status {
        SUCCESS,
        NOTNEEDED,
        ERROR,
        NOTFOUND,
        ABORTED,
        DENIED,
        INVALID
    }
}
