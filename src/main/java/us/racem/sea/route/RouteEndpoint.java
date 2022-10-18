package us.racem.sea.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.ArrayUtils;
import us.racem.sea.body.Response;
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

    private Pattern param(String name) {
        for (var seg: segments) {
            if (seg.equals(name)) return seg.ptrn();
        }

        return null;
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

    private int kindOf(Parameter par) {
        var type = par.getAnnotatedType();
        var header = type.getAnnotation(NamedHeader.class);
        var param = type.getAnnotation(NamedParam.class);
        var body = type.getAnnotation(Body.class);

        if (!xor(header, param, body)) return -1;

        if (header != null) return 0;
        if (param != null) return 1;
        if (body != null) return 2;

        return -1;
    }

    private static class ThunkInvoker {
        private static class InternalThunks {
            public static final MethodHandle PARAMETER_THUNK;
            public static final MethodHandle HEADER_THUNK;

            static {
                try {
                    PARAMETER_THUNK = unreflect(InternalThunks.class.getDeclaredMethod(
                            "paramThunk", Pattern.class, String.class), null);
                    HEADER_THUNK = unreflect(InternalThunks.class.getDeclaredMethod(
                            "headerThunk", String.class, Map.class), null);
                } catch (IllegalAccessException | NoSuchMethodException err) {
                    throw new RuntimeException(err);
                }
            }

            private static Object paramThunk(Pattern ptrn, String path) {
                var regex = ptrn.matcher(path);
                return regex.group(0);
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

        private int pos;

        public ThunkInvoker(MethodHandle target) {
            this.thunks = new ArrayList<>();
            this.target = target;
            this.zero = MethodHandles.constant(Object.class, null);
            this.pos = 0;
        }

        public ThunkInvoker header(String name) {
            var target = InternalThunks.HEADER_THUNK
                            .bindTo(name);
            thunks.add(new Thunk(pos, target, ThunkKind.HEADER));
            this.pos = pos + 1;

            return this;
        }

        public ThunkInvoker param(Pattern ptrn) {
            var target = InternalThunks.PARAMETER_THUNK
                    .bindTo(ptrn);
            thunks.add(new Thunk(pos, target, ThunkKind.PARAM));
            this.pos = pos + 1;

            return this;
        }

        public ThunkInvoker body() {
            var target = MethodHandles.identity(byte[].class);
            thunks.add(new Thunk(pos, target, ThunkKind.BODY));
            this.pos = pos + 1;

            return this;
        }

        public ThunkInvoker zero() {
            var target = zero;
            thunks.add(new Thunk(pos, zero, ThunkKind.ZERO));
            this.pos = pos + 1;

            return this;
        }

        public Object invoke(String path, Map<String, List<String>> headers, byte[] body) throws Throwable {
            var res = new ArrayList<>();
            for (var thunk: thunks) {
                res.add(switch (thunk.kind) {
                    case HEADER -> thunk.target.invoke(headers);
                    case PARAM -> thunk.target.invoke(path);
                    case BODY -> thunk.target.invoke(body);
                    case ZERO -> thunk.target.invoke();
                });
            }

            return target.invoke(res.toArray());
        }
    }

    private static <T> T[] dup(int n, T val) {
        var arr = new Object[n];
        Arrays.fill(arr, val);
        return (T[]) arr;
    }

    private record MetaObject(int headers, int params, int body) {};
    private static Response wrapper(String mime, MethodHandle receiver,
                                    MetaObject meta,
                                    String path,
                                    Map<String, List<String>> headers,
                                    byte[] body)
            throws Throwable {
        if (meta.headers > 1) {
            receiver.bindTo(dup(meta.headers, headers));
        }

        if (meta.params > 1) {
            receiver.bindTo(dup(meta.params, path));
        }

        if (meta.body > 1) {
            receiver.bindTo(dup(meta.body, body));
        }

        var obj = receiver.invoke();
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
        var params = ordered(receiver);
        var fn = reorder(receiver, inst);

        for (int i = 0; i < params.length; i++)  {
            var param = params[i];
            var name = nameOf(param);
            var kind = kindOf(param);
            if ((kind == 0 || kind == 1) && name == null) continue;

            switch (kind) {
                case 0 -> {
                    var thunk = unreflect(Thunks
                                    .class
                                    .getDeclaredMethod("headerThunk", String.class, Map.class),
                            null)
                            .bindTo(name);
                    fn = MethodHandles.foldArguments(fn, thunk);
                }

                case 1 -> {
                    var ptrn= param(name);
                    var thunk = unreflect(Thunks
                                .class
                                .getDeclaredMethod("paramThunk", Pattern.class, String.class),
                            null)
                            .asType(methodType(param.getType(), Pattern.class, String.class))
                            .bindTo(ptrn);
                    fn = MethodHandles.foldArguments(fn, 0, thunk);
                }
            }
        }

        if (numberOf(receiver, "body") > 1) {
            var thunk = unreflect(Thunks
                    .class
                    .getDeclaredMethod("bodyThunk", int.class, byte[].class),
                    null)
                    .bindTo(numberOf(receiver, "body"));
            fn = MethodHandles.collectArguments(fn, 0, thunk);
        }

        var pos = 0;
        if ((pos = numberOf(receiver, "headers")) > 1) {
            fn = fn.asSpreader(pos, Map[].class, numberOf(receiver, "headers"));
        }

        if ((pos = pos + numberOf(receiver, "params")) > 1) {
            fn = fn.asSpreader(pos, String[].class, numberOf(receiver, "params"));
        }

        if ((pos = pos + numberOf(receiver, "body")) > 1) {
            fn = fn.asSpreader(pos, byte[][].class, numberOf(receiver, "body"));
        }

        return unreflect(RouteEndpoint.class
                        .getDeclaredMethod("wrapper",
                                String.class, MethodHandle.class,
                                MetaObject.class,
                                String.class, Map.class,
                                byte[].class),
                null)
                .bindTo(mime)
                .bindTo(fn)
                .bindTo(new MetaObject(
                    numberOf(receiver, "headers"),
                    numberOf(receiver, "params"),
                    numberOf(receiver, "body")
                ));
    }

    private int numberOf(Method fn, String what) {
        var ord = switch (what) {
            case "headers" -> 0;
            case "params" -> 1;
            case "body" -> 3;
            case null, default -> -1;
        };

        var count = 0;
        var params = fn.getParameters();

        for (int i = 0; i < params.length; i++) {
            var param = params[i];
            var name = nameOf(param);
            var kind = kindOf(param);
            if ((kind == 0 || kind == 1) && name == null) continue;
            if (kind == ord) count++;
        }

        return count;
    }

    private Parameter[] ordered(Method fn) {
        var params = fn.getParameters();

        var headerClasses = new HashSet<Parameter>();
        var paramClasses = new HashSet<Parameter>();
        var bodyClasses = new HashSet<Parameter>();
        var otherClasses = new HashSet<Parameter>();

        for (var param : params) {
            var name = nameOf(param);
            var kind = kindOf(param);
            if ((kind == 0 || kind == 1) && name == null) continue;
            switch (kind) {
                case 0 -> headerClasses.add(param);
                case 1 -> paramClasses.add(param);
                case 3 -> bodyClasses.add(param);
                case -1 -> otherClasses.add(param);
            }
        }

        return union(headerClasses, paramClasses, bodyClasses, otherClasses)
                .toArray(new Parameter[0]);
    }

    private MethodHandle reorder(Method receiver, Object receiverObj) throws IllegalAccessException {
        var params = receiver.getParameters();

        var headerIndices = new HashSet<Integer>();
        var paramIndices = new HashSet<Integer>();
        var bodyIndices = new HashSet<Integer>();
        var otherIndices = new HashSet<Integer>();

        for (int i = 0; i < params.length; i++) {
            var param = params[i];
            var name = nameOf(param);
            var kind = kindOf(param);
            if ((kind == 0 || kind == 1) && name == null) continue;

            var idx = i + 1;
            switch (kind) {
                case 0 -> headerIndices.add(idx);
                case 1 -> paramIndices.add(idx);
                case 3 -> bodyIndices.add(idx);
                case -1 -> otherIndices.add(idx);
            }
        }

        var prefixIndices = Set.of(0);
        Integer[] indices;
        if (!isStatic(receiver)) {
            indices = union(
                    prefixIndices,
                    headerIndices, paramIndices,
                    bodyIndices, otherIndices
            ).toArray(new Integer[0]);
        } else {
            indices = union(
                    headerIndices, paramIndices,
                    bodyIndices, otherIndices
            ).toArray(new Integer[0]);
        }

        var prefix = of(klass);
        var kind = methodType(receiver.getReturnType(), union(prefix, receiver.getParameterTypes()));

        return MethodHandles.permuteArguments(
                unreflect(receiver, receiverObj),
                kind, ArrayUtils.toPrimitive(indices))
                .bindTo(inst);
    }
}
