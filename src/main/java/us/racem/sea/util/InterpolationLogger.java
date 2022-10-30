package us.racem.sea.util;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InterpolationLogger extends Logger {
    public static class InterpolationLoggerFactory implements LoggerFactory {
        public InterpolationLoggerFactory() {}

        public Logger makeNewLoggerInstance (String name) {
            return new InterpolationLogger(name);
        }
    }

    public static String FQCN = InterpolationLogger.class.getName() + ".";
    private static final InterpolationLoggerFactory factory = new InterpolationLoggerFactory();

    public InterpolationLogger(String name) {
        super(name);
    }

    public static InterpolationLogger getLogger(Class clazz) {
        return (InterpolationLogger) LogManager.getLogger(clazz.getName(), factory);
    }

    private final String regexStr = "(\\{(?<idx>\\d|[a-f])?})";
    private final Pattern regexPtrn = Pattern.compile(regexStr);
    private final StackWalker walker = StackWalker
            .getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private String getLogPrefix() throws IllegalAccessException {
        var source = walker.walk(frames -> frames
                .map(StackWalker.StackFrame::getDeclaringClass)
                .skip(3)
                .findFirst()
        ).orElse(null);
        if (source == null) return "";

        var prefix = Arrays.stream(source.getDeclaredFields()).filter(f -> f.getName().equals("logPrefix")).findFirst().orElse(null);
        if (prefix == null) return "";
        if (!Modifier.isStatic(prefix.getModifiers())) return "";
        if (!prefix.getType().isAssignableFrom(String.class)) return "";
        if (Modifier.isPrivate(prefix.getModifiers())) prefix.setAccessible(true);

        return String.format("[%s] ", prefix.get(null));
    }

    private String fmt(String message, Object... fmt) throws IllegalAccessException {
        var regex = regexPtrn.matcher(message);

        var finalStr = new StringBuilder(getLogPrefix());
        var curIdx = 0;
        while (regex.find()) {
            if (regex.start() == -1) {
                continue;
            }

            var fmtPos = regex.start("idx") > 0 ? regex.group("idx").charAt(0): -1;
            var fmtIdx = fmtPos > 0 ? Character.digit(fmtPos, 16) : -1;
            if (fmtIdx >= fmt.length || curIdx >= fmt.length) {
                continue;
            }

            var fmtStr = fmtIdx >= 0 ? String.valueOf(fmt[fmtIdx]) : String.valueOf(fmt[curIdx]);
            var finalFmtStr = Matcher.quoteReplacement(fmtStr);
            regex.appendReplacement(finalStr, finalFmtStr);

            curIdx++;
        }

        regex.appendTail(finalStr);
        return finalStr.toString();
    }

    // Boilerplate grr
    public void warn(String message, Object... objs) {
        try {
            var fmtMessage = fmt(message, objs);
            super.log(FQCN, Level.WARN, fmtMessage, null);
        } catch (Exception e) { super.error("[Meta]: Normal Logger had error: ", e); }
    }

    public void info(String message, Object... objs) {
        try {
            var fmtMessage = fmt(message, objs);
            super.log(FQCN, Level.INFO, fmtMessage, null);
        } catch (Exception e) { super.error("[Meta]: Normal Logger had error: ", e); }
    }

    public void error(String message, Object... objs) {
        try {
            var fmtMessage = fmt(message, objs);
            super.log(FQCN, Level.ERROR, fmtMessage, null);
        } catch (Exception e) { super.error("[Meta]: Normal Logger had error: ", e); }
    }

    // No substitution

    public void warn(Object message) {
        try {
            var fmtMessage = fmt(String.valueOf(message));
            super.log(FQCN, Level.WARN, fmtMessage, null);
        } catch (Exception e) { super.error("[Meta]: Normal Logger had error: ", e); }
    }

    public void info(Object message) {
        try {
            var fmtMessage = fmt(String.valueOf(message));
            super.log(FQCN, Level.INFO, fmtMessage, null);
        } catch (Exception e) { super.error("[Meta]: Normal Logger had error: ", e); }    }

    public void error(Object message) {
        try {
            var fmtMessage = fmt(String.valueOf(message));
            super.log(FQCN, Level.ERROR, fmtMessage, null);
        } catch (Exception e) { super.error("[Meta]: Normal Logger had error: ", e); }    }
}