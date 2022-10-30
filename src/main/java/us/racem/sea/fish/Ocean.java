package us.racem.sea.fish;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import us.racem.sea.inject.RouteCodecInjector;
import us.racem.sea.inject.RouteMappingInjector;
import us.racem.sea.net.SeaServer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Ocean {
    public static void fill() {
        fill(ConfigFactory.load());
    }

    public static void fill(Config config) {
        config.checkValid(ConfigFactory.defaultReference(), "sea");
        var prefix = config.getString("sea.prefix");
        var port = config.getInt("sea.port");

        var max_header_size = config.getInt("sea.max_header_size");
        var max_body_size = config.getInt("sea.max_body_size");

        var server = new SeaServer(port, max_body_size, max_header_size);
        var injector = new OceanInjector(prefix,
                RouteCodecInjector.class,
                RouteMappingInjector.class);

        injector.run();
        server.run();
    }
}
