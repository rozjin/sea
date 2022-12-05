package us.racem.sea.fish;

import com.google.inject.Guice;
import com.google.inject.Module;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import us.racem.sea.inject.RouteCodecInjector;
import us.racem.sea.inject.RouteMappingInjector;
import us.racem.sea.net.SeaServer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Ocean {
    private Config config;

    public Ocean(Config config, Module... modules) {
        init(config, modules);
    }

    public void init(Config config, Module... modules) {
        if (config == null) config = ConfigFactory.load();
        config.checkValid(ConfigFactory.defaultReference(), "sea");
        var prefix = config.getString("sea.prefix");
        var port = config.getInt("sea.port");

        var max_header_size = config.getInt("sea.max_header_size");
        var max_body_size = config.getInt("sea.max_body_size");

        var server = new SeaServer(port, max_body_size, max_header_size);
        var injector = new OceanInjector(prefix,
                Guice.createInjector(modules),

                RouteCodecInjector.class,
                RouteMappingInjector.class);

        injector.run();
        server.run();
    }
}
