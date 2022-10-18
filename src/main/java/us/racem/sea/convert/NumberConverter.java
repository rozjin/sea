package us.racem.sea.convert;

import us.racem.sea.mark.inject.PathConverter;

@PathConverter("num")
public class NumberConverter extends AnyConverter {
    @Override
    public String regex() {
        return "[-+]?\\d+";
    }
}
