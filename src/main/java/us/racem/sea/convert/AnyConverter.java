package us.racem.sea.convert;

public abstract class AnyConverter<T> {
    public abstract String regex();
    public abstract T convert(String text);
}