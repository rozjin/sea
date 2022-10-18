package us.racem.sea.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.ArrayUtils;
import us.racem.sea.body.Response;
import us.racem.sea.convert.AnyConverter;
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
import static us.racem.sea.util.MethodUtils.isStatic;
import static us.racem.sea.util.MethodUtils.unreflect;
import static us.racem.sea.util.SetUtils.*;

public class RouteEndpoint {
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

    public RouteEndpoint(RouteSegment segment, Method receiver) {
        this.segments = segments(segment);
        this.klass = receiver.getDeclaringClass();

        this.path = path(receiver);
        this.methods = methods(receiver);
        this.mime = mime(receiver);

        try {
            if (path == null ||
                methods == null ||
                mime == null) throw new NoSuchMethodException("Invalid Route");
            this.inst = klass
                    .getDeclaredConstructor()
                    .newInstance();
            this.bound = bind(receiver);
        } catch (NoSuchMethodException | InstantiationException |
                 IllegalAccessException | InvocationTargetException err) {
            logger.info("Failed to Initialise Route {}", receiver);
        }
    }

    public boolean handles(RequestMethod method) {
        return ArrayUtils.contains(methods, method);
    }
    public MethodHandle bound() {
        return bound;
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

    private AnyConverter<?> conv(Pattern ptrn) {
        for (var conv: Router.converters().values()) {
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

    private enum TargetParameterKind { HEADER, PARAM, BODY, OTHER };

    private TargetParameterKind kindOf(Parameter par) {
        var type = par.getAnnotatedType();
        var header = type.getAnnotation(NamedHeader.class);
        var param = type.getAnnotation(NamedParam.class);
        var body = type.getAnnotation(Body.class);

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
                            "paramThunk", Pattern.class, AnyConverter.class, String.class), null);
                    HEADER_THUNK = unreflect(InternalThunks.class.getDeclaredMethod(
                            "headerThunk", String.class, Map.class), null);
                } catch (IllegalAccessException | NoSuchMethodException err) {
                    throw new RuntimeException(err);
                }
            }

            private static Object paramThunk(Pattern ptrn, AnyConverter<?> conv, String seg) {
                var regex = ptrn.matcher(seg);
                if (!regex.find()) return 0;

                return conv.convert(seg);
            }

            private static List<String> headerThunk(String name, Map<String, List<String>> headers) {
                if (headers == null) return null;
                return headers.get(name);
            }
        }

        private enum ThunkKind { PARAM, HEADER, BODY, ZERO };

        private record Thunk(int pos, MethodHandle target, ThunkKind kind) {}
        private List<Thunk> thunks;
        private MethodHandle target;
        private MethodHandle zero;
        private boolean built;

        public ThunkInvoker(MethodHandle target) {
            this.thunks = new ArrayList<>();
            this.target = target;
            this.built = false;
            this.zero = MethodHandles.constant(Object.class, null);
        }

        public ThunkInvoker header(String name) {
            var target = InternalThunks.HEADER_THUNK
                            .bindTo(name);
            thunks.add(new Thunk(-1, target, ThunkKind.HEADER));
            return this;
        }

        public ThunkInvoker param(Pattern ptrn, AnyConverter<?> conv, int pos) {
            var target = InternalThunks.PARAMETER_THUNK
                    .bindTo(ptrn)
                    .bindTo(conv);
            thunks.add(new Thunk(pos, target, ThunkKind.PARAM));
            return this;
        }

        public ThunkInvoker body() {
            var target = MethodHandles.identity(byte[].class);
            thunks.add(new Thunk(-1, target, ThunkKind.BODY));
            return this;
        }

        public ThunkInvoker zero() {
            var target = zero;
            thunks.add(new Thunk(-1, zero, ThunkKind.ZERO));
            return this;
        }

        public Object invoke(String path, Map<String, List<String>> headers, byte[] body) throws Throwable {
            var parts = path.split("/{1,2}");
            var res = new ArrayList<>();

            for (var thunk : thunks) {
                res.add(switch (thunk.kind) {
                    case HEADER -> thunk.target.invoke(headers);
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

    private static <T> T[] dup(int n, T val) {
        var arr = new Object[n];
        Arrays.fill(arr, val);
        return (T[]) arr;
    }

    private static Response wrapper(ThunkInvoker invoker,
                                    String mime,
                                    String path,
                                    Map<String, List<String>> headers,
                                    byte[] body)
            throws Throwable {

        var obj = invoker.invoke(path, headers, body);
        return switch (obj) {
            case Response rsp -> {
                var bytes = switch (mime) {
                    case Mime.Plain, Mime.HTML -> switch (rsp.body) {
                        case String sng -> sng.getBytes(StandardCharsets.UTF_8);
                        case default, null -> null;
                    };

                    case Mime.JSON -> mapper
                            .writeValueAsString(rsp.body)
                            .getBytes(StandardCharsets.UTF_8);

                    case default, Mime.Binary -> switch (rsp.body) {
                        case byte[] $ -> $;
                        case default, null -> null;
                    };
                };

                yield Response.from(rsp.status, bytes, rsp.mime, rsp.headers);
            }

            default -> {
                var bytes = switch (mime) {
                    case Mime.Plain, Mime.HTML -> switch (obj) {
                        case String sng -> sng.getBytes(StandardCharsets.UTF_8);
                        case default -> null;
                    };

                    case Mime.JSON -> mapper
                            .writeValueAsString(obj)
                            .getBytes(StandardCharsets.UTF_8);

                    case default, Mime.Binary -> switch (obj) {
                        case byte[] $ -> $;
                        case default -> null;
                    };
                };

                yield Response.of(200, bytes, mime);
            }

            case null -> {
                logger.info("No response! {}", path);
                yield Response.of(500, "{}", Mime.JSON);
            }
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
                    if (param.getType() != List.class) {
                        invoker.zero();
                        continue;
                    }

                    invoker.header(name);
                }
                case PARAM -> {
                    var pos = pos(name);
                    var ptrn = ptrn(name);
                    var conv = conv(ptrn);
                    if (pos == -1 || conv == null) {
                        invoker.zero();
                        continue;
                    }

                    invoker.param(ptrn, conv, pos);
                }
                case BODY -> {
                    if (param.getType() != byte[].class) {
                        invoker.zero();
                        continue;
                    }

                    invoker.body();
                }
                case OTHER -> invoker.zero();
            }
        }

        return unreflect(RouteEndpoint.class
                        .getDeclaredMethod("wrapper",
                                ThunkInvoker.class,
                                String.class, String.class,
                                Map.class, byte[].class), null)
                .bindTo(invoker.build())
                .bindTo(mime);
    }
}
