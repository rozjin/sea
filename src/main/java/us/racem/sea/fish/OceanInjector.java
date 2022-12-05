package us.racem.sea.fish;

import com.google.inject.Injector;
import org.reflections.Reflections;
import us.racem.sea.inject.AnyInjector;
import us.racem.sea.util.InterpolationLogger;

import java.lang.reflect.InvocationTargetException;

public class OceanInjector extends OceanExecutable {
    private static final InterpolationLogger logger = InterpolationLogger.getLogger(Ocean.class);
    private static final String logPrefix = "INJ";

    private final String main;
    private final Class<? extends AnyInjector>[] classes;
    private final Injector injector;

    @SafeVarargs
    public OceanInjector(String prefix,
                         Injector injector,
                         Class<? extends AnyInjector>... classes) {
        this.main = prefix;
        this.injector = injector;
        this.classes = classes;
    }

    @Override
    public void run() {
        for (var clazz: classes) {
            try {
                logger.info("Inject: {}", clazz.getSimpleName());
                var instance = clazz.getDeclaredConstructor(String.class)
                               .newInstance(main);
                injector.injectMembers(instance);
                instance.inject();
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException |
                    NoSuchMethodException err) {
                logger.warn("Failed to Initialize injector: {}", err);
            }
        }
    }
}
