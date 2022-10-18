package us.racem.sea.convert;

import us.racem.sea.mark.inject.PathConverter;

import java.util.UUID;

@PathConverter("uuid")
public class UUIDConverter extends AnyConverter<UUID> {
    @Override
    public String regex() {
        return "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    }

    @Override
    public UUID convert(String text) {
        return UUID.fromString(text);
    }
}
