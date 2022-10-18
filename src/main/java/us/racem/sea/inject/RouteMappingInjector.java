package us.racem.sea.inject;

import org.reflections.scanners.Scanners;
import us.racem.sea.fish.Ocean;
import us.racem.sea.mark.methods.*;
import us.racem.sea.route.Router;
import us.racem.sea.util.InterpolationLogger;

import java.lang.reflect.Method;
import java.util.Set;

import static us.racem.sea.util.SetUtils.union;
import static us.racem.sea.util.SetUtils.xor;

public class RouteMappingInjector extends AnyInjector {
    private static final InterpolationLogger logger = InterpolationLogger.getLogger(Ocean.class);
    private static final String logPrefix = "PTH";

    public RouteMappingInjector(String main) {
        super(main, Scanners.TypesAnnotated, Scanners.SubTypes, Scanners.MethodsAnnotated);
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

    @Override
    public void inject() {
        var receivers = find();
        logger.info("Methods: {}", receivers);

        for (var receiver: receivers) {
            var route = path(receiver);

            Router.route(route, receiver);
            logger.info("Registered Route: {}", route);
        }
    }
}
