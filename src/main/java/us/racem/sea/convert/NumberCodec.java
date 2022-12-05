package us.racem.sea.convert;

import us.racem.sea.mark.inject.Codec;

@Codec("num")
public class NumberCodec extends AnyCodec<Long> {
    public NumberCodec() {
        super("\\d+");
    }

    @Override
    public Long decode(String text) {
        return Long.valueOf(text);
    }

    @Override
    public String encode(Long obj) {
        return String.valueOf(obj);
    }
}
