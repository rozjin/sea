package us.racem.sea.convert;

public abstract class AnyCodec<T> {
    private final String regex;

    public AnyCodec(String regex) {
        this.regex = "(" + regex + ")";
    }

    public String regex() {
        return regex;
    }

    public abstract T decode(String text);
    public abstract String encode(T obj);
}