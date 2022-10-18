package us.racem.sea.inject;

import us.racem.sea.convert.AnyConverter;
import us.racem.sea.fish.Ocean;
import us.racem.sea.mark.inject.PathConverter;
import us.racem.sea.route.Router;
import us.racem.sea.util.InterpolationLogger;

import java.lang.reflect.InvocationTargetException;

public class ConverterInjector extends AnyInjector {
    private static final InterpolationLogger logger = InterpolationLogger.getLogger(Ocean.class);
    private static final String logPrefix = "CNV";

    public ConverterInjector(String main) {
        super(main);
    }

    @Override
    public void inject() {
        var converterClasses = reflector
                .getSubTypesOf(AnyConverter.class)
                .stream().filter(convertClass -> convertClass.isAnnotationPresent(PathConverter.class))
                .toList();

        for (var converterClass: converterClasses) {
            try {
                var converter = converterClass
                        .getDeclaredConstructor()
                        .newInstance();
                var name = converterClass.getAnnotation(PathConverter.class).value();

                Router.converter(name, converter);
                logger.info("Registered Converter: {}", converterClass.getSimpleName());
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException |
                     NoSuchMethodException err) {
                logger.warn("Failed to Initialize Converter: {}", err);
            }
        }
    }
}
