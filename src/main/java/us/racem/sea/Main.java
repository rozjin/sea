package us.racem.sea;

import us.racem.sea.fish.Ocean;
import us.racem.sea.mark.body.Content;
import us.racem.sea.mark.body.NoContent;
import us.racem.sea.mark.methods.GetMapping;
import us.racem.sea.mark.request.param.NamedParam;
import us.racem.sea.util.InterpolationLogger;

public class Main {
    private static final InterpolationLogger logger = InterpolationLogger.getLogger(Main.class);
    private static final String logPrefix = "Main";

    @GetMapping(path = "/api/v/[version: num]")
    private String rootHandler(@NamedParam("version") String version) {
        return "test";
    }

    public static void main(String[] arr) {
        var ocean = new Ocean();
        ocean.main(arr);
    }
}
