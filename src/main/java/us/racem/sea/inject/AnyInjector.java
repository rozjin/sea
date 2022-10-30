package us.racem.sea.inject;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

public abstract class AnyInjector {
    public Reflections reflector;
    public AnyInjector(String prefix, Scanners... scanners) {
        this.reflector = new Reflections(prefix, scanners);
    }

    public abstract void inject();
}
