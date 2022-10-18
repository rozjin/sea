package us.racem.sea.fish;

import us.racem.sea.inject.ConverterInjector;
import us.racem.sea.inject.ErrorMappingInjector;
import us.racem.sea.inject.RouteMappingInjector;
import us.racem.sea.net.SeaServer;
import us.racem.sea.route.Router;
import us.racem.sea.util.ArgumentParser;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Ocean {
    private final ExecutorService executor;

    public Ocean() {
        this.executor = Executors.newFixedThreadPool(4);
    }

    public void main(String[] arr) {
        var args = ArgumentParser.parse(arr);
        var main = args.getOrDefault("main", "us.racem.sea");

        var server = new SeaServer(8080);
        var injector = new InjectorExecutor(main,
                ConverterInjector.class,
                ErrorMappingInjector.class,
                RouteMappingInjector.class);

        injector.run();
        server.run();
    }
}
