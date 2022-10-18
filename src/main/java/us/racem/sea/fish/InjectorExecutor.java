package us.racem.sea.fish;

import org.reflections.Reflections;
import us.racem.sea.inject.AnyInjector;
import us.racem.sea.util.InterpolationLogger;

import java.lang.reflect.InvocationTargetException;

public class InjectorExecutor extends OceanExecutable {
    private static final InterpolationLogger logger = InterpolationLogger.getLogger(Ocean.class);
    private static final String logPrefix = "INJ";

    private final Reflections reflector;
    private final String main;
    private final Class<? extends AnyInjector>[] injectorClasses;

    @SafeVarargs
    public InjectorExecutor(String main,
                            Class<? extends AnyInjector>... injectorClasses) {
        this.reflector = new Reflections("us.racem.sea.inject");
        this.main = main;
        this.injectorClasses = injectorClasses;
    }

    @Override
    public void run() {
        for (var injectorClass: injectorClasses) {
            try {
                logger.info("Called: {}", injectorClass.getSimpleName());
                injectorClass
                        .getDeclaredConstructor(String.class)
                        .newInstance(main)
                        .inject();
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException |
                    NoSuchMethodException err) {
                logger.warn("Failed to Initialize injector: {}", err);
            }
        }
    }
}
