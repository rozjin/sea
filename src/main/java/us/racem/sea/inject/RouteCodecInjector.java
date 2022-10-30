package us.racem.sea.inject;

import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import us.racem.sea.convert.AnyCodec;
import us.racem.sea.fish.Ocean;
import us.racem.sea.mark.inject.PathConverter;
import us.racem.sea.route.RouteRegistry;
import us.racem.sea.util.InterpolationLogger;

import java.lang.reflect.InvocationTargetException;

import static org.reflections.scanners.Scanners.*;

public class RouteCodecInjector extends AnyInjector {
    private static final InterpolationLogger logger = InterpolationLogger.getLogger(Ocean.class);
    private static final String logPrefix = "CNV";

    public RouteCodecInjector(String prefix) {
        super(prefix);
        this.reflector = new Reflections(new ConfigurationBuilder()
                .forPackages("us.racem.sea", prefix)
                .setScanners(TypesAnnotated, SubTypes, MethodsAnnotated));
    }

    @Override
    public void inject() {
        var converterClasses = reflector
                .getSubTypesOf(AnyCodec.class)
                .stream().filter(convertClass -> convertClass.isAnnotationPresent(PathConverter.class))
                .toList();

        for (var converterClass: converterClasses) {
            try {
                var converter = converterClass
                        .getDeclaredConstructor()
                        .newInstance();
                var name = converterClass.getAnnotation(PathConverter.class).value();

                RouteRegistry.register(name, converter);
                logger.info("Registered Converter: {}", converterClass.getSimpleName());
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException |
                     NoSuchMethodException err) {
                logger.warn("Failed to Initialize Converter: {}", err);
            }
        }
    }
}
