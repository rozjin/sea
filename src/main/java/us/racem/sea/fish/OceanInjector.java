package us.racem.sea.fish;

import org.reflections.Reflections;
import us.racem.sea.inject.AnyInjector;
import us.racem.sea.util.InterpolationLogger;

import java.lang.reflect.InvocationTargetException;

public class OceanInjector extends OceanExecutable {
    private static final InterpolationLogger logger = InterpolationLogger.getLogger(Ocean.class);
    private static final String logPrefix = "INJ";

    private final String main;
    private final Class<? extends AnyInjector>[] injectorClasses;

    @SafeVarargs
    public OceanInjector(String prefix,
                         Class<? extends AnyInjector>... injectorClasses) {
        this.main = prefix;
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
