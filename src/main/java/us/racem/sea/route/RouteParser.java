package us.racem.sea.route;

import us.racem.sea.fish.Ocean;
import us.racem.sea.util.InterpolationLogger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static us.racem.sea.util.SetUtils.*;

public class RouteParser {
    private static final InterpolationLogger logger = InterpolationLogger.getLogger(Ocean.class);
    private static final String logPrefix = "PRS";

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
    private enum MatchKind {
        STRING,
        PATTERN
    }
    private enum MatchSep {
        AND,
        NONE
    }

    public static MatchObj matchOf(Matcher regex, MatchObj other) {
        var sep = regex.group("SEP") == null
                ? MatchSep.NONE
                : MatchSep.AND;

        var kind = regex.group("PARAM") == null
                ? MatchKind.STRING
                : MatchKind.PATTERN;

        var name = switch (kind) {
            case STRING -> null;
            case PATTERN -> "(" + regex.group("NAME") + ")";
        };

        var ptrn = switch (kind) {
            case STRING -> regex.group("PART");
            case PATTERN -> Router.converters().get(regex.group("KIND"));
        };

        if (other != null) ptrn = other.ptrn + "(" + ptrn + ")";
        return new MatchObj(name, ptrn, kind, sep, other);
    }

    public static RouteSegment parse(String path, Method receiver) {
        if (Objects.equals(path, "/") ||
            Objects.equals(path, "//")) {
            return in("/", Router.routes())
                    ? take("/", Router.routes())
                    : new RouteSegment("/", receiver);
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

        if (matchers.get(0).kind != MatchKind.STRING) return null;

        var pathMatchers = slice(matchers, 1, (match) -> match.sep == MatchSep.AND);
        var queryMatchers = slice(matchers, (match) -> match.sep == MatchSep.AND);

        RouteSegment root;
        if (in(matchers.get(0).ptrn, Router.routes())) {
            root = take(matchers.get(0).ptrn, Router.routes());
        } else {
            if (matchers.size() == 1) {
                return new RouteSegment(matchers.get(0).ptrn, receiver);
            }

            root = new RouteSegment(matchers.get(0).ptrn);
        }

        var pivot = root;
        for (var pathMatcher : pathMatchers) {
            pivot = pivot.leaf(pathMatcher.ptrn);
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
            pivot = pivot.leaf(ptrn);
        }
        pivot.bind(receiver);

        return root;
    }
}
