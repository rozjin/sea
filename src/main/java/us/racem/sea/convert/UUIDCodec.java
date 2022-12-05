package us.racem.sea.convert;

import us.racem.sea.mark.inject.Codec;

import java.util.UUID;

@Codec("uuid")
public class UUIDCodec extends AnyCodec<UUID> {
    public UUIDCodec() {
        super("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Override
    public UUID decode(String text) {
        return UUID.fromString(text);
    }

    @Override
    public String encode(UUID obj) {
        return obj.toString();
    }
}
