package us.racem.sea.mark.methods;

public enum RequestMethod {
    GET,

    POST,
    PUT,
    PATCH,

    DELETE;

    public static RequestMethod of(String method) {
        return switch (method) {
            case "GET" -> GET;
            case "POST" -> POST;
            case "PUT" -> PUT;
            case "PATCH" -> PATCH;

            case null, default -> null;
        };
    }
}
