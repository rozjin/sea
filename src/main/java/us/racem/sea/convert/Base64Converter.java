package us.racem.sea.convert;

import us.racem.sea.mark.inject.PathConverter;

@PathConverter("base64")
public class Base64Converter extends AnyConverter {
    @Override
    public String regex() {
        return "(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?";
    }
}
