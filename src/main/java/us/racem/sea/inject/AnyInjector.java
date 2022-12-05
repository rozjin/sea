package us.racem.sea.inject;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

public abstract class AnyInjector {
    @Inject
    public Injector injector;

    public Reflections reflector;
    public AnyInjector(String prefix, Scanners... scanners) {
        this.reflector = new Reflections(prefix, scanners);
    }

    public abstract void inject();
}
