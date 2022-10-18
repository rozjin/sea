package us.racem.sea.convert;

import us.racem.sea.mark.inject.PathConverter;

@PathConverter("string")
public class StringConverter extends AnyConverter {
    @Override
    public String regex() {
        return "[^/&=]";
    }
}
