package us.racem.sea.route;

import us.racem.sea.body.Request;
import us.racem.sea.convert.AnyConverter;
import us.racem.sea.fish.Ocean;
import us.racem.sea.body.Response;
import us.racem.sea.mark.body.Mime;
import us.racem.sea.mark.methods.RequestMethod;
import us.racem.sea.util.InterpolationLogger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import static us.racem.sea.util.MethodUtils.*;
import static us.racem.sea.util.SetUtils.in;
import static us.racem.sea.util.SetUtils.take;

public class Router {
    private static final InterpolationLogger logger = InterpolationLogger.getLogger(Ocean.class);
    private static final String logPrefix = "ROT";

    private static final List<RouteSegment> routes = new ArrayList<>();
    private static final Map<String, RouteEndpoint> cache = new HashMap<>();
    private static final Map<Integer, MethodHandle> errors = new HashMap<>();
    private static final Map<String, AnyConverter<?>> converters = new HashMap<>();

    private static final String delimRegexStr = "(?<!^)/{1,2}";
    private static final Pattern delimRegexPtrn = Pattern.compile(delimRegexStr, Pattern.MULTILINE);

    public static Map<String, AnyConverter<?>> converters() {
        return converters;
    }

    public static List<RouteSegment> routes() {
        return routes;
    }

    public static void converter(String name, AnyConverter<?> conv) {
        converters.put(name, conv);
    }

    public static void route(String route, Method receiver) {
        var matcher = RouteParser.parse(route, receiver);
        if (matcher == null) {
            logger.warn("%rFailed to parse route {}%", route);
            return;
        }

        routes.add(matcher);
    }

    public static void error(Integer status, MethodHandle receiver) {
        errors.put(status, receiver);
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

    private static RouteEndpoint lookup(String path) {
        if (path.isBlank() ||
            Objects.equals(path, "/") ||
            Objects.equals(path, "//")) {
            if (!in("/", routes)) return null;

            var pivot = take("/", routes);
            return pivot.bound();
        }

        if (cache.containsKey(path)) return cache.get(path);

        var parts = delimRegexPtrn.split(path);
        if (parts.length == 0) return null;
        if (!in(parts[0], routes, (e, w) -> e.ptrn().matcher(w).matches())) return null;

        var pivot = take(parts[0], routes);
        for (int i = 1; i < parts.length; i++) {
            if (pivot == null) return null;
            var part = parts[i];
            pivot = lookupSegment(pivot, part);
        }

        if (pivot == null) return null;
        cache.put(path, pivot.bound());

        return pivot.bound();
    }

    private static Response err(String path, int status) {
        try {
            var err = errors.get(status);
            if (err == null) return Response.of(500, "{}", Mime.JSON);

            return (Response) err.invoke(path);
        } catch (Throwable err) {
            logger.error("%rError Handler {} had error {}%", "/" + path, err);
        }

        return Response.of(500, "{}", Mime.JSON);
    }

    public static Response request(RequestMethod method,
                                   String path,
                                   Map<String, List<String>> headers,
                                   byte[] body) {
        var endpoint = lookup(path);
        if (endpoint == null) {
            logger.warn("%yRoute not found {}%", "/" + path);
            return err(path, 404);
        }

        if (!endpoint.handles(method)) {
            logger.warn("%yInvalid Method {} on route {}%", method, "/" + path);
            return err(path, 405);
        }

        else {
            try {
                var handle = endpoint.bound();
                var res = handle.invoke(path, headers, body);

                return (Response) res;
            } catch (Throwable err) {
                logger.error("%rRoute {} had error {}%", "/" + path, err);
                err.printStackTrace();
                return err(path, 500);
            }
        }
    }
}
