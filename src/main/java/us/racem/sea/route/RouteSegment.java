package us.racem.sea.route;

import us.racem.sea.fish.Ocean;
import us.racem.sea.util.InterpolationLogger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static us.racem.sea.util.SetUtils.in;
import static us.racem.sea.util.SetUtils.take;

public class RouteSegment {
    private static final InterpolationLogger logger = InterpolationLogger.getLogger(Ocean.class);
    private static final String logPrefix = "SEG";

    private final String segment;
    private final Pattern segmentPtrn;
    private final RouteSegment prev;
    private final List<RouteSegment> next;

    private RouteEndpoint endpoint;
    public RouteSegment(String segment) {
        this.segment = segment;
        this.segmentPtrn = Pattern.compile(segment);
        this.prev = null;
        this.endpoint = null;
        this.next = new ArrayList<>();
    }

    public RouteSegment(String segment, Method receiver) {
        this.segment = segment;
        this.segmentPtrn = Pattern.compile(segment);
        this.prev = null;
        this.endpoint = new RouteEndpoint(this, receiver);
        this.next = new ArrayList<>();
    }

    public RouteSegment(String segment, RouteSegment prev) {
        this.segment = segment;
        this.segmentPtrn = Pattern.compile(segment);
        this.prev = prev;
        this.endpoint = null;
        this.next = new ArrayList<>();
    }

    public RouteSegment(String segment, RouteSegment prev, Method receiver) {
        this.segment = segment;
        this.segmentPtrn = Pattern.compile(segment);
        this.prev = prev;
        this.endpoint = new RouteEndpoint(this, receiver);
        this.next = new ArrayList<>();
    }

    public RouteSegment prev() {
        return prev;
    }

    public List<RouteSegment> succ() {
        return next;
    }

    public Pattern ptrn() {
        return segmentPtrn;
    }

    public RouteSegment leaf(String segment) {
        if (!in(segment, next)) {
            var node = new RouteSegment(segment, this);
            next.add(node);

            return node;
        }

        return take(segment, next);
    }

    public RouteSegment leaf(String segment,
                             Method receiver) {
        if (!in(segment, next)) {
            var node = new RouteSegment(segment, this, receiver);
            next.add(node);

            return node;
        }

        return take(segment, next);
    }

    public RouteEndpoint bound() {
        return endpoint;
    }

    public void bind(Method receiver) {
        this.endpoint = new RouteEndpoint(this, receiver);
    }

    @Override
    public boolean equals(Object other) {
        return switch (other) {
            case String seg -> seg.equals(segment);
            case RouteSegment node -> node.segment.equals(segment);
            default -> false;
        };
    }

    private String stringify(RouteSegment node) {
        if (node.next == null || node.next.size() == 0) {
            return node.segment;
        }

        var spacer = " -> ";
        var treeBuilder = new StringBuilder();
        treeBuilder
                .append(node.segment)
                .append(spacer);

        for (var succ: node.succ()) {
            treeBuilder.append(stringify(succ));
        }

        return treeBuilder.toString();
    }

    @Override
    public String toString() {
        return stringify(this);
    }
}