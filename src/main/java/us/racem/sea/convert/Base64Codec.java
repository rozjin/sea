package us.racem.sea.convert;

import org.apache.commons.codec.binary.Base64;
import us.racem.sea.mark.inject.Codec;

@Codec("base64")
public class Base64Codec extends AnyCodec<byte[]> {
    public Base64Codec() {
        super("(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?");
    }

    @Override
    public byte[] decode(String text) {
        return Base64.decodeBase64(text);
    }

    @Override
    public String encode(byte[] obj) {
        return Base64.encodeBase64String(obj);
    }
}
