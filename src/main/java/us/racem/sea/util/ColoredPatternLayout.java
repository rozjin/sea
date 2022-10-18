package us.racem.sea.util;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColoredPatternLayout extends PatternLayout {
    private static final Map<Character, Integer> colorMap = Map.of(
            'l', 30,
            'r', 31,
            'g', 32,
            'y', 33,
            'b', 34,
            'm', 35,
            'p', 35,
            'c', 36,
            'w', 37,
            'd', 39
    );
    private static final Map<Character, Integer> optMap = Map.of(
            'b', 1,
            'i', 3,
            'u', 4,
            't', 9
    );

    private static final String FMT_PREFIX = "\u001b[";
    private static final String FMT_SUFFIX = "m";
    private static final int FMT_RST = 0;
    private static final char FMT_SEP = ';';
    private static final String RST_SEQ = FMT_PREFIX + FMT_RST + FMT_SUFFIX;

    private final String regexStr = "((?<color>%[a-z])(?<opt>\\{[a-z]+})?(?<txt>[^%]*)%)";
    private final Pattern regexPtrn = Pattern.compile(regexStr);

    private String fmt(LoggingEvent src) {
        var srcStr = super.format(src);

        var regex = regexPtrn.matcher(srcStr);
        var finalStr = new StringBuilder();
        while (regex.find()) {
            if (regex.start() == -1) {
                continue;
            }

            var txt = srcStr.substring(regex.start("txt"), regex.end("txt"));
            var color = srcStr.charAt(regex.start("color") + 1);
            if (color == 'n') {
                regex.appendReplacement(finalStr, RST_SEQ + txt);
                continue;
            }

            if (!colorMap.containsKey(color)) {
                src.getLogger().warn("Format String malformed; color: " + color + " does not exist");
                regex.appendReplacement(finalStr, RST_SEQ + txt);
                continue;
            }

            var optPos = regex.start("opt");
            if (optPos == -1) {
                regex.appendReplacement(finalStr, FMT_PREFIX + colorMap.get(color) + FMT_SUFFIX + txt + RST_SEQ);
                continue;
            }

            var fmtStr = new StringBuilder(txt)
                    .insert(0, FMT_SUFFIX)
                    .insert(0, colorMap.get(color));

            var optEnd = regex.end("opt") - 1;
            var opts = srcStr.substring(optPos + 1, optEnd);
            for (char opt : opts.toCharArray()) {
                if (!optMap.containsKey(opt)) {
                    continue;
                }

                fmtStr.insert(0, FMT_SEP);
                fmtStr.insert(0, optMap.get(opt));
            }

            fmtStr.insert(0, FMT_PREFIX);
            fmtStr.append(RST_SEQ);

            var finalFmtStr = Matcher.quoteReplacement(fmtStr.toString());
            regex.appendReplacement(finalStr, finalFmtStr);
        }

        regex.appendTail(finalStr);
        return finalStr.toString();
    }

    @Override
    public String format(LoggingEvent src) {
        return fmt(src);
    }
}
