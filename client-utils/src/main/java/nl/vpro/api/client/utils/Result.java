package nl.vpro.api.client.utils;

/**
 * @author Michiel Meeuwissen
 * @since 1.0
 */
public class Result {

    private final boolean success;

    private final String errors;

    private Result(boolean success, String errors) {
        this.success = success;
        this.errors = errors;
    }

    public static Result success() {
        return new Result(true, null);
    }
    public static Result error(String message) {
        return new Result(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return success + ":" + errors;
    }
}
