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

    public boolean isSuccess() {
        return status != Status.ERROR && status != Status.ABORTED;
    }
    public Status getStatus() {
        return status;
    }

    public String getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return status + ":" + errors;
    }

    public enum Status {
        SUCCESS,
        ERROR,
        NOTFOUND,
        ABORTED,
        DENIED
    }
}
