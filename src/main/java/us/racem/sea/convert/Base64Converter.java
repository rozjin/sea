package us.racem.sea.convert;

import org.apache.commons.codec.binary.Base64;
import us.racem.sea.mark.inject.PathConverter;

@PathConverter("base64")
public class Base64Converter extends AnyConverter<byte[]> {
    @Override
    public String regex() {
        return "(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?";
    }

    @Override
    public byte[] convert(String text) {
        return Base64.decodeBase64(text);
    }
}
