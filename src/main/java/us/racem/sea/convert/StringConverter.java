package us.racem.sea.convert;

import us.racem.sea.mark.inject.PathConverter;

@PathConverter("string")
public class StringConverter extends AnyConverter<String> {
    @Override
    public String regex() {
        return "[^/&=]";
    }

    @Override
    public String convert(String text) {
        return text;
    }
}
