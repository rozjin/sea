package us.racem.sea.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.primitives.Primitives;
import org.apache.commons.lang3.ArrayUtils;
import us.racem.sea.body.Response;
import us.racem.sea.convert.AnyCodec;
import us.racem.sea.fish.Ocean;
import us.racem.sea.mark.body.*;
import us.racem.sea.mark.methods.*;
import us.racem.sea.mark.request.body.Body;
import us.racem.sea.mark.request.header.NamedHeader;
import us.racem.sea.mark.request.param.NamedParam;
import us.racem.sea.util.InterpolationLogger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

import static java.lang.invoke.MethodType.methodType;
import static us.racem.sea.util.MethodUtils.unreflect;
import static us.racem.sea.util.SetUtils.of;
import static us.racem.sea.util.SetUtils.xor;

public class RouteInvoker {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final InterpolationLogger logger = InterpolationLogger.getLogger(Ocean.class);
    private static final String logPrefix = "ROU";

    private Class<?> klass;
    private MethodHandle bound;

    private Object inst;

    private String path;
    private RequestMethod[] methods;
    private String mime;

    private List<RouteSegment> segments;

    public RouteInvoker(RouteSegment segment, Method receiver, Object instance) {
        this.segments = segments(segment);
        this.klass = receiver.getDeclaringClass();

        this.path = path(receiver);
        this.methods = methods(receiver);
        this.mime = mime(receiver);

        try {
            if (path == null ||
                methods == null ||
                mime == null) throw new NoSuchMethodException("Invalid Route");
            this.inst = instance;
            this.bound = bind(receiver);
        } catch (NoSuchMethodException |
                 IllegalAccessException err) {
            logger.info("Failed to Initialise Route {}", receiver);
        }
    }

    public boolean handles(RequestMethod method) {
        return ArrayUtils.contains(methods, method);
    }

    private List<RouteSegment> segments(RouteSegment segment) {
        var patterns = new ArrayList<RouteSegment>();
        for (var pivot = segment; pivot != null; pivot = pivot.prev()) {
            patterns.add(pivot);
        }

        return patterns;
    }

    private Pattern ptrn(String name) {
        for (var seg: segments) {
            if (seg.nameEquals(name)) return seg.ptrn();
        }

        return null;
    }

    private AnyCodec<?> conv(Pattern ptrn) {
        for (var conv: RouteRegistry.converters.values()) {
            if (conv.regex().equals(ptrn.toString())) return conv;
        }

        return null;
    }

    private int pos(String name) {
        var segments = Lists.reverse(this.segments);
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i).nameEquals(name)) return i;
        }

        return -1;
    }

    private String path(Method receiver) {
        var request = receiver.getAnnotation(RequestMapping.class);

        var get = receiver.getAnnotation(GetMapping.class);
        var put = receiver.getAnnotation(PutMapping.class);
        var patch = receiver.getAnnotation(PatchMapping.class);
        var post = receiver.getAnnotation(PostMapping.class);
        var delete = receiver.getAnnotation(DeleteMapping.class);

        if (!xor(request, get, put, patch, post, delete)) return null;

        if (request != null) return request.path();

        if (get != null) return get.path();
        if (put != null) return put.path();
        if (patch != null) return patch.path();
        if (post != null) return post.path();
        if (delete != null) return delete.path();

        throw new RuntimeException("Unreachable!");
    }

    private RequestMethod[] methods(Method receiver) {
        var request = receiver.getAnnotation(RequestMapping.class);

        var get = receiver.getAnnotation(GetMapping.class);
        var put = receiver.getAnnotation(PutMapping.class);
        var patch = receiver.getAnnotation(PatchMapping.class);
        var post = receiver.getAnnotation(PostMapping.class);
        var delete = receiver.getAnnotation(DeleteMapping.class);

        if (!xor(request, get, put, patch, post, delete)) return null;

        if (request != null) return request.methods();

        if (get != null) return of(RequestMethod.GET);
        if (put != null) return of(RequestMethod.PUT);
        if (patch != null) return of(RequestMethod.PATCH);
        if (post != null) return of(RequestMethod.POST);
        if (delete != null) return of(RequestMethod.DELETE);

        throw new RuntimeException("Unreachable!");
    }

    private String mime(Method receiver) {
        var type = receiver.getAnnotatedReturnType();
        if (type.getType() == void.class) return Mime.Binary;

        var content = type.getAnnotation(Content.class);

        var html = type.getAnnotation(HtmlContent.class);
        var json = type.getAnnotation(JsonContent.class);
        var none = type.getAnnotation(NoContent.class);

        if (!xor(content, html, json, none)) return Mime.Plain;

        if (content != null) return content.mime();

        if (html != null) return Mime.HTML;
        if (json != null) return Mime.JSON;
        if (none != null) return Mime.Binary;

        return Mime.Plain;
    }

    private String nameOf(Parameter par) {
        var type = par.getAnnotatedType();
        var header = type.getAnnotation(NamedHeader.class);
        var param = type.getAnnotation(NamedParam.class);

        if (!xor(header, param)) return null;

        if (header != null) return header.value();
        if (param != null) return param.value();

        return null;
    }

    private enum TargetParameterKind { HEADER, PARAM, METHOD, BODY, OTHER };

    private TargetParameterKind kindOf(Parameter par) {
        var type = par.getAnnotatedType();
        var header = type.getAnnotation(NamedHeader.class);
        var param = type.getAnnotation(NamedParam.class);
        var body = type.getAnnotation(Body.class);

        if (par.getType() == RequestMethod.class) return TargetParameterKind.METHOD;
        if (!xor(header, param, body)) return TargetParameterKind.OTHER;

        if (header != null) return TargetParameterKind.HEADER;
        if (param != null) return TargetParameterKind.PARAM;
        if (body != null) return TargetParameterKind.BODY;

        return TargetParameterKind.OTHER;
    }

    private static class ThunkInvoker {
        private static class InternalThunks {
            public static final MethodHandle PARAMETER_THUNK;
            public static final MethodHandle HEADER_THUNK;

            static {
                try {
                    PARAMETER_THUNK = unreflect(InternalThunks.class.getDeclaredMethod(
                            "paramThunk", Pattern.class, AnyCodec.class, String.class), null);
                    HEADER_THUNK = unreflect(InternalThunks.class.getDeclaredMethod(
                            "headerThunk", String.class, Map.class), null);
                } catch (IllegalAccessException | NoSuchMethodException err) {
                    throw new RuntimeException(err);
                }
            }

            private static Object paramThunk(Pattern ptrn, AnyCodec<?> conv, String seg) {
                var regex = ptrn.matcher(seg);
                if (!regex.matches()) return null;

                return conv.decode(regex.group());
            }

            private static Object headerThunk(String name, Map<String, List<String>> headers) {
                if (headers == null) return null;
                var res = headers.get(name);
                if (res.size() == 1) return res.get(0);
                return res;
            }
        }

        private enum ThunkKind { PARAM, METHOD, HEADER, BODY, ZERO }

        private record Thunk(int pos, MethodHandle target, ThunkKind kind) {}
        private List<Thunk> thunks;
        private MethodHandle target;
        private boolean built;

        public ThunkInvoker(MethodHandle target) {
            this.thunks = new ArrayList<>();
            this.target = target;
            this.built = false;
        }

        public ThunkInvoker header(String name) {
            var target = InternalThunks.HEADER_THUNK
                            .bindTo(name);
            thunks.add(new Thunk(-1, target, ThunkKind.HEADER));
            return this;
        }

        public ThunkInvoker param(Pattern ptrn, AnyCodec<?> conv, int pos) {
            var target = InternalThunks.PARAMETER_THUNK
                    .bindTo(ptrn)
                    .bindTo(conv);
            thunks.add(new Thunk(pos, target, ThunkKind.PARAM));
            return this;
        }

        public ThunkInvoker method() {
            var target = MethodHandles.identity(RequestMethod.class);
            thunks.add(new Thunk(-1, target, ThunkKind.METHOD));
            return this;
        }

        public ThunkInvoker body() {
            var target = MethodHandles.identity(byte[].class);
            thunks.add(new Thunk(-1, target, ThunkKind.BODY));
            return this;
        }

        public ThunkInvoker zero(Class<?> kind) {
            var wrappedKind = Primitives.wrap(kind);
            var isNumeric = Number.class.isAssignableFrom(wrappedKind);

            thunks.add(new Thunk(-1,
                    MethodHandles.constant(kind, isNumeric ? -1 : null),
                    ThunkKind.ZERO));
            return this;
        }

        public Object invoke(String path, RequestMethod method, Map<String, List<String>> headers, byte[] body) throws Throwable {
            var parts = path.split("/{1,2}");
            var res = new ArrayList<>();

            for (var thunk : thunks) {
                res.add(switch (thunk.kind) {
                    case HEADER -> thunk.target.invoke(headers);
                    case METHOD -> thunk.target.invoke(method);
                    case PARAM -> thunk.target.invoke(parts[thunk.pos]);
                    case BODY -> thunk.target.invoke(body);
                    case ZERO -> thunk.target.invoke();
                });
            }

            return target.invoke(res.toArray());
        }

        public ThunkInvoker build() {
            if (built) throw new RuntimeException("Thunk has been built!");
            this.built = true;
            this.target = target
                    .asSpreader(0, Object[].class, target.type().parameterCount());
            return this;
        }
    }

    private static Response wrapper(ThunkInvoker invoker,
                                    String mime,
                                    String path, RequestMethod method,
                                    Map<String, List<String>> headers,
                                    byte[] body)
            throws Throwable {
        var obj = invoker.invoke(path, method, headers, body);
        return switch (obj) {
            case Response rsp -> rsp;
            case String sng -> new Response(200,
                    sng.getBytes(StandardCharsets.UTF_8),
                    mime);
            case default, null -> RouteRegistry.errorOf(path, "Invalid Response from handler.", 500);
        };
    }

    private MethodHandle bind(Method receiver) throws
            NoSuchMethodException,
            IllegalAccessException {
        var params = receiver.getParameters();
        var fn = unreflect(receiver, inst);
        var invoker = new ThunkInvoker(fn);

        for (var param : params) {
            var name = nameOf(param);
            var kind = kindOf(param);
            if ((kind == TargetParameterKind.PARAM || kind == TargetParameterKind.HEADER) && name == null) continue;

            switch (kind) {
                case HEADER -> {
                    if (param.getType() != List.class &&
                        param.getType() != String.class) {
                        invoker.zero(param.getType());
                        continue;
                    }

                    invoker.header(name);
                }
                case PARAM -> {
                    var pos = pos(name);
                    var ptrn = ptrn(name);
                    if (ptrn ==  null) {
                        invoker.zero(param.getType());
                        continue;
                    }

                    var conv = conv(ptrn);
                    if (pos == -1 || conv == null) {
                        invoker.zero(param.getType());
                        continue;
                    }

                    invoker.param(ptrn, conv, pos);
                }
                case METHOD -> {
                    if (param.getType() != RequestMethod.class) {
                        invoker.zero(param.getType());
                        continue;
                    }

                    invoker.method();
                }
                case BODY -> {
                    if (param.getType() != byte[].class) {
                        invoker.zero(param.getType());
                        continue;
                    }

                    invoker.body();
                }
                case OTHER -> invoker.zero(param.getType());
            }
        }

        return unreflect(RouteInvoker.class
                        .getDeclaredMethod("wrapper",
                                ThunkInvoker.class,
                                String.class,
                                String.class, RequestMethod.class,
                                Map.class, byte[].class), null)
                .bindTo(invoker.build())
                .bindTo(mime);
    }

    public Response invoke(String path,
                           RequestMethod method,
                           Map<String, List<String>> headers,
                           byte[] body) throws Throwable {
        return (Response) bound.invoke(path, method, headers, body);
    }
}
