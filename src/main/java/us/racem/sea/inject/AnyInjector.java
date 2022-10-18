package us.racem.sea.inject;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

public abstract class AnyInjector {
    public final Reflections reflector;
    public AnyInjector(String main, Scanners... scanners) {
        this.reflector = new Reflections(main, scanners);
    }

    public abstract void inject();
}
