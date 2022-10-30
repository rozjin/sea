package us.racem.sea.convert;

import us.racem.sea.mark.inject.PathConverter;

@PathConverter("string")
public class StringCodec extends AnyCodec<String> {
    public StringCodec() {
        super("[^/&=]");
    }

    @Override
    public String decode(String text) {
        return text;
    }

    @Override
    public String encode(String obj) {
        return obj;
    }
}
