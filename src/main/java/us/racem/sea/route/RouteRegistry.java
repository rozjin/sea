package us.racem.sea.route;

import us.racem.sea.body.Response;
import us.racem.sea.convert.AnyCodec;
import us.racem.sea.fish.Ocean;
import us.racem.sea.mark.body.Mime;
import us.racem.sea.mark.methods.RequestMethod;
import us.racem.sea.util.InterpolationLogger;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

import static us.racem.sea.util.SetUtils.in;
import static us.racem.sea.util.SetUtils.take;

public class RouteRegistry {
    private static final InterpolationLogger logger = InterpolationLogger.getLogger(Ocean.class);
    private static final String logPrefix = "ROT";

    public record Route(RouteSegment root, Pattern prefix) {
        @Override
        public boolean equals(Object obj) {
            return switch (obj) {
                case String ptrn -> ptrn.equals(prefix.toString());
                case Pattern ptrn -> ptrn.equals(prefix);
                default -> false;
            };
        }
    }
    static final List<Route> tries = new ArrayList<>();
    static final Map<String, AnyCodec<?>> converters = new HashMap<>();

    private static final String delimRegexStr = "(?<!^)/{1,2}";
    private static final Pattern delimRegexPtrn = Pattern.compile(delimRegexStr, Pattern.MULTILINE);

    public static void register(String name, AnyCodec<?> conv) {
        converters.put(name, conv);
    }

    public static void register(String route, Method receiver, Object instance) {
        var matcher = RouteParser.segmentsOf(route, receiver, instance);
        var prefix = RouteParser.prefixOf(route);
        if (matcher == null) {
            logger.warn("%rFailed to parse route {}%", route);
            return;
        }

        tries.add(new Route(matcher, prefix));
    }

    private static RouteSegment lookupSegment(RouteSegment pivot, String part) {
        if (pivot == null) return null;
        for (var next: pivot.succ()) {
            var regex = next
                    .ptrn()
                    .matcher(part);
            if (regex.matches()) return next;
        }

        return null;
    }

    private static RouteInvoker lookup(String path) {
        if (path.isBlank() ||
            Objects.equals(path, "/") ||
            Objects.equals(path, "//")) {
            if (!in("/", tries)) return null;

            var pivot = take("/", tries).root();
            return pivot.bound();
        }

        var parts = delimRegexPtrn.split(path);
        if (parts.length == 0) return null;
        if (!in(parts[0], tries, (e, w) -> e.prefix.matcher(w).matches())) return null;

        var pivot = take(parts[0], tries).root();
        for (int i = 1; i < parts.length; i++) {
            if (pivot == null) return null;
            var part = parts[i];
            pivot = lookupSegment(pivot, part);
        }

        if (pivot == null) return null;
        return pivot.bound();
    }

    public static Response errorOf(String path, String message, int status) {
        return new Response(500, "{" +
                "err: \"" + message + "\", " +
                "route: \"" + path + "\", " +
                "rayId: \"" + UUID.randomUUID() + "\"" +
                "}", Mime.JSON);
    }

    public static Response requestOf(String path,
                                     RequestMethod method,
                                     Map<String, List<String>> headers,
                                     byte[] body) {
        var endpoint = lookup(path);
        if (endpoint == null) {
            logger.warn("%yRoute not found {}%", "/" + path);
            return errorOf(path, "No handler for route.", 500);
        }

        if (!endpoint.handles(method)) {
            logger.warn("%yInvalid Method {} on route {}%", method, "/" + path);
            return errorOf(path, "Unsupported method.",405);
        }

        else {
            try {
                return endpoint.invoke(path, method, headers, body);
            } catch (Throwable err) {
                logger.error("%rRoute {} had error {}%", "/" + path, err);
                err.printStackTrace();
                return errorOf(path, "Internal application error", 500);
            }
        }
    }
}
