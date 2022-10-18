package us.racem.sea.convert;

import us.racem.sea.mark.inject.PathConverter;

@PathConverter("uuid")
public class UUIDConverter extends AnyConverter {
    @Override
    public String regex() {
        return "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    }
}
