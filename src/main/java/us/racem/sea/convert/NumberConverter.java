package us.racem.sea.convert;

import us.racem.sea.mark.inject.PathConverter;

@PathConverter("num")
public class NumberConverter extends AnyConverter<Integer> {
    @Override
    public String regex() {
        return "[-+]?\\d+";
    }

    @Override
    public Integer convert(String text) {
        return Integer.valueOf(text);
    }
}
