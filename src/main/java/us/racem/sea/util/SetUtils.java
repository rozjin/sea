package us.racem.sea.util;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SetUtils {
    public static <T> T[] of(T... elem) {
        return elem;
    }

    @SafeVarargs
    public static <T> Set<T> union(Set<T>... sets) {
        return Stream.of(sets)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @SafeVarargs
    public static <T> T[] union(T[] first, T[]... rest) {
        var len = first.length;
        for (var array : rest) {
            len = len + array.length;
        }

        var res = Arrays.copyOf(first, len);
        var off = first.length;
        for (var array : rest) {
            System.arraycopy(array, 0, res, off, array.length);
            off = off + array.length;
        }

        return res;
    }

    public static <E, T> boolean in(E what, List<T> list) {
        for (var elem: list) {
            if (elem.equals(what)) return true;
        }

        return false;
    }

    public static <E, T> boolean in(E what, List<T> list, BiFunction<T, E, Boolean> condition) {
        for (var elem: list) {
            if (condition.apply(elem, what)) return true;
        }

        return false;
    }

    public static <E, T> T take(E what, List<T> list) {
        for (var elem: list) {
            if (elem.equals(what)) return elem;
        }

        return null;
    }

    public static <T> List<T> slice(List<T> original, int startPos, int endPos) {
        var res = new ArrayList<T>();
        if (startPos == -1 || endPos == -1 || startPos > original.size() || endPos > original.size() || startPos > endPos) return res;
        res.addAll(original.subList(startPos, endPos));

        return res;
    }

    public static <T> List<T> slice(List<T> original, int startPos, Function<T, Boolean> condition) {
        var endPos = posOf(original, condition);
        if (endPos == -1) return slice(original, startPos, original.size());
        return slice(original, startPos, endPos);
    }

    public static <T> List<T> slice(List<T> original, Function<T, Boolean> condition) {
        return slice(original, posOf(original, condition), original.size());
    }

    public static <T> int posOf(List<T> original, Function<T, Boolean> condition) {
        for (int i = 0; i < original.size(); i++) {
            var elem = original.get(i);
            if (condition.apply(elem)) return i;
        }

        return -1;
    }


    public static boolean xor(Object... terms) {
        return Arrays.stream(terms).filter(Objects::nonNull).count() == 1;
    }
}
