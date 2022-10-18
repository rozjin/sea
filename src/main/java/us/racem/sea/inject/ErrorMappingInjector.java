package us.racem.sea.inject;

import org.reflections.scanners.Scanners;
import us.racem.sea.fish.Ocean;
import us.racem.sea.mark.methods.ErrorMapping;
import us.racem.sea.mark.methods.GetMapping;
import us.racem.sea.route.Router;
import us.racem.sea.util.InterpolationLogger;

import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import static us.racem.sea.util.MethodUtils.*;

public class ErrorMappingInjector extends AnyInjector {
    private static final InterpolationLogger logger = InterpolationLogger.getLogger(Ocean.class);
    private static final String logPrefix = "PTH";

    public ErrorMappingInjector(String main) {
        super(main, Scanners.TypesAnnotated, Scanners.SubTypes, Scanners.MethodsAnnotated);
    }

    private Set<Method> find() {
        return reflector.getMethodsAnnotatedWith(ErrorMapping.class);
    }

    @Override
    public void inject() {
        var errors = find();
        logger.info("Error Methods: {}", errors);

        for (var error: errors) {
            try {
                if (error.getReturnType() != String.class) throw new WrongMethodTypeException("Error Handlers must return String!");
                var errorObj = error
                        .getDeclaringClass()
                        .getDeclaredConstructor()
                        .newInstance();
                var status = error.getDeclaredAnnotation(ErrorMapping.class).value();

                Router.error(status, unreflect(error, errorObj));
                logger.info("Registered Error Handler: {}", status);
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException |
                     NoSuchMethodException err) {
                logger.warn("Failed to Initialize Error Handler: {}", err);
            }
        }
    }
}
