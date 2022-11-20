package us.racem.sea.inject;

import us.racem.sea.fish.Ocean;
import us.racem.sea.mark.methods.*;
import us.racem.sea.route.RouteRegistry;
import us.racem.sea.util.InterpolationLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;

import static org.reflections.scanners.Scanners.*;
import static us.racem.sea.util.SetUtils.union;
import static us.racem.sea.util.SetUtils.xor;

public class RouteMappingInjector extends AnyInjector {
    private static final InterpolationLogger logger = InterpolationLogger.getLogger(Ocean.class);
    private static final String logPrefix = "PTH";

    public RouteMappingInjector(String prefix) {
        super(prefix, TypesAnnotated, SubTypes, MethodsAnnotated);
    }

    private Set<Method> find() {
        var request = reflector.getMethodsAnnotatedWith(RequestMapping.class);

        var get = reflector.getMethodsAnnotatedWith(GetMapping.class);
        var put = reflector.getMethodsAnnotatedWith(PutMapping.class);
        var patch = reflector.getMethodsAnnotatedWith(PatchMapping.class);
        var post = reflector.getMethodsAnnotatedWith(PostMapping.class);
        var delete = reflector.getMethodsAnnotatedWith(DeleteMapping.class);

        return union(request, get, put, patch, post, delete);
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

    private String prefix(Method receiver) {
        var klass = receiver.getDeclaringClass();
        var prefix = klass.getAnnotation(RequestPrefix.class);
        if (prefix != null) return prefix.value();
        return "";
    }

    @Override
    public void inject() {
        var receivers = find();
        var instances = new HashMap<Class<?>, Object>();
        logger.info("Methods: {}", receivers);

        for (var receiver: receivers) {
            try {
                var prefix = prefix(receiver);
                var route = prefix + path(receiver);
                var instance = instances.computeIfAbsent(receiver.getDeclaringClass(), ($) -> {
                    try {
                        return receiver.getDeclaringClass()
                                .getDeclaredConstructor()
                                .newInstance();
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                             NoSuchMethodException err) {
                        throw new RuntimeException(err);
                    }
                });


                RouteRegistry.register(route, receiver, instance);
                logger.info("Registered Route: {}", route);
            } catch (Exception err) {
                logger.warn("Failed to register Route: {}", err);
            }
        }
    }
}
