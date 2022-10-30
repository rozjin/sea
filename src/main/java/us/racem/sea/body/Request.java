package us.racem.sea.body;

import java.util.List;
import java.util.Map;

public record Request(
        String path,
        int op,
        Map<String, List<String>> headers,
        byte[] body) {}
