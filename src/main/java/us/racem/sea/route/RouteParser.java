package us.racem.sea.route;

import us.racem.sea.fish.Ocean;
import us.racem.sea.util.InterpolationLogger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static us.racem.sea.util.SetUtils.*;

public class RouteParser {
    private static final InterpolationLogger logger = InterpolationLogger.getLogger(Ocean.class);
    private static final String logPrefix = "PRS";

    private static final String routeRegexStr = """
            (
                (/{1,2})
                (
                    ([^?\\#/\\[\\]]+)|
                    (\\[
                        (?<NAME>[^?|/\\[\\]]+)
                        (?:\\s*):(?:\\s*)
                        (?<KIND>[^?|/\\[\\]]+)
                    ]
                    (?<SEP>
                        (?:\\s*)
                        ,
                        (?:\\s*)
                    )?
                    )
                )
            )+
            """;
    private static final Pattern routeRegexPtrn = Pattern.compile(routeRegexStr,
            Pattern.MULTILINE | Pattern.COMMENTS);
    private static final String partRegexStr = """
            (?<PART>[^?\\#/\\[\\]]+)|
            (?<PARAM>
              \\[
                (?<NAME>[^?|/\\[\\]]+)
                (?:\\s*):(?:\\s*)
                (?<KIND>[^?|/\\[\\]]+)
              ]
              (?<SEP>
                (?:\\s*)
                ,
                (?:\\s*)
              )?
            )
            """;

    private static final Pattern partRegexPtrn = Pattern.compile(partRegexStr,
            Pattern.MULTILINE | Pattern.COMMENTS);
    private static final String delimRegexStr = "/{1,2}";
    private static final Pattern delimRegexPtrn = Pattern.compile(delimRegexStr, Pattern.MULTILINE);

    private record MatchObj(String name, String ptrn, MatchKind kind, MatchSep sep, MatchObj other) {}
    private enum MatchKind { STRING, PATTERN }
    private enum MatchSep { AND, NONE }

    public static MatchObj matchOf(Matcher regex, MatchObj other) {
        var sep = regex.group("SEP") == null
                ? MatchSep.NONE
                : MatchSep.AND;

        var kind = regex.group("PARAM") == null
                ? MatchKind.STRING
                : MatchKind.PATTERN;

        var name = switch (kind) {
            case STRING -> regex.group("PART");
            case PATTERN -> regex.group("NAME");
        };

        var ptrn = switch (kind) {
            case STRING -> regex.group("PART");
            case PATTERN -> RouteRegistry.converters.get(regex.group("KIND")).regex();
        };

        if (other != null) ptrn = other.ptrn + "(" + ptrn + ")";
        return new MatchObj(name, ptrn, kind, sep, other);
    }

    private static RouteSegment make(List<MatchObj> matchers, Method receiver,
            Object instance) {
        var ptrn = matchers.get(0).ptrn;
        var name = matchers.get(0).name;

        if (in(ptrn, RouteRegistry.tries)) {
            return take(ptrn, RouteRegistry.tries).root();
        } else {
            if (matchers.size() == 1) {
                return new RouteSegment(name, ptrn, null, receiver, instance);
            }

            return new RouteSegment(name, ptrn, null, null, null);
        }
    }

    public static Pattern prefixOf(String path) {
        if (Objects.equals(path, "/") ||
                Objects.equals(path, "//")) {
            return Pattern.compile("/");
        }
        var matchers = new ArrayList<MatchObj>();
        for (var part: delimRegexPtrn.split(path)) {
            var regex = partRegexPtrn.matcher(part);
            var match = (MatchObj) null;

            while (regex.find()) {
                if (regex.start() == -1) {
                    return null;
                }

                match = matchOf(regex, match);
            }

            if (match != null) matchers.add(match);
        }

        return Pattern.compile(matchers.get(0).ptrn);
    }

    public static RouteSegment segmentsOf(String path, Method receiver,
                                          Object instance) {
        if (Objects.equals(path, "/") ||
            Objects.equals(path, "//")) {
            return in("/", RouteRegistry.tries)
                    ? take("/", RouteRegistry.tries).root()
                    : new RouteSegment("/", "/", null, receiver, instance);
        }

        var routePtrnRegex = routeRegexPtrn.matcher(path);
        if (!routePtrnRegex.matches()) return null;

        var matchers = new ArrayList<MatchObj>();
        for (var part: delimRegexPtrn.split(path)) {
            var regex = partRegexPtrn.matcher(part);
            var match = (MatchObj) null;

            while (regex.find()) {
                if (regex.start() == -1) {
                    return null;
                }

                match = matchOf(regex, match);
            }

            if (match != null) matchers.add(match);
        }

        if (matchers.get(0).kind != MatchKind.STRING) return null;

        var pathMatchers = slice(matchers, 1, (match) -> match.sep == MatchSep.AND);
        var queryMatchers = slice(matchers, (match) -> match.sep == MatchSep.AND);

        var root = make(matchers, receiver, instance);
        var pivot = root;
        for (var pathMatcher : pathMatchers) {
            pivot = pivot.fork(pathMatcher.name, pathMatcher.ptrn);
        }

        for (int i = 0; i < queryMatchers.size(); i++) {
            var queryMatcher = queryMatchers.get(i);

            var name = queryMatcher.name;
            var ptrn = switch (i) {
                case 0 -> queryMatcher.ptrn;
                default -> switch (queryMatcher.kind) {
                    case STRING -> null;
                    case PATTERN -> i == 1
                        ? "\\?" + name + "=" + queryMatcher.ptrn
                        : "&" + name + "=" + queryMatcher.ptrn;
                };
            };

            if (ptrn == null) return null;
            pivot = pivot.fork(name, ptrn);
        }
        pivot.bind(receiver, instance);

        return root;
    }
}
