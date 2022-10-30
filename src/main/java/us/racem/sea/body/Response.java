package us.racem.sea.body;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Response {
    private static final Map<Integer, String> opcodes = new HashMap<>(){{
        put(100, "Continue");
        put(101, "Switching Protocols");
        put(102, "Processing");
        put(103, "Early Hints");
        put(200, "OK");
        put(201, "Created");
        put(202, "Accepted");
        put(203, "Non-Authoritative Information");
        put(204, "No Content");
        put(205, "Reset Content");
        put(206, "Partial Content");
        put(207, "Multi-Status (WebDAV)");
        put(208, "Already Reported (WebDAV)");
        put(226, "IM Used");
        put(300, "Multiple Choices");
        put(301, "Moved Permanently");
        put(302, "Found");
        put(303, "See Other");
        put(304, "Not Modified");
        put(305, "Use Proxy");
        put(306, "(Unused)");
        put(307, "Temporary Redirect");
        put(308, "Permanent Redirect (experimental)");
        put(400, "Bad Request");
        put(401, "Unauthorized");
        put(402, "Payment Required");
        put(403, "Forbidden");
        put(404, "Not Found");
        put(405, "Method Not Allowed");
        put(406, "Not Acceptable");
        put(407, "Proxy Authentication Required");
        put(408, "Request Timeout");
        put(409, "Conflict");
        put(410, "Gone");
        put(411, "Length Required");
        put(412, "Precondition Failed");
        put(413, "Request Entity Too Large");
        put(414, "Request-URI Too Long");
        put(415, "Unsupported Media Type");
        put(416, "Requested Range Not Satisfiable");
        put(417, "Expectation Failed");
        put(418, "I'm a teapot (RFC 2324)");
        put(420, "Enhance Your Calm (Twitter)");
        put(422, "Unprocessable Entity (WebDAV)");
        put(423, "Locked (WebDAV)");
        put(424, "Failed Dependency (WebDAV)");
        put(425, "Reserved for WebDAV");
        put(426, "Upgrade Required");
        put(428, "Precondition Required");
        put(429, "Too Many Requests");
        put(431, "Request Header Fields Too Large");
        put(444, "No Response (Nginx)");
        put(449, "Retry With (Microsoft)");
        put(450, "Blocked by Windows Parental Controls (Microsoft)");
        put(451, "Unavailable For Legal Reasons");
        put(499, "Client Closed Request (Nginx)");
        put(500, "Internal Server Error");
        put(501, "Not Implemented");
        put(502, "Bad Gateway");
        put(503, "Service Unavailable");
        put(504, "Gateway Timeout");
        put(505, "HTTP Version Not Supported");
        put(506, "Variant Also Negotiates (Experimental)");
        put(507, "Insufficient Storage (WebDAV)");
        put(508, "Loop Detected (WebDAV)");
        put(509, "Bandwidth Limit Exceeded (Apache)");
        put(510, "Not Extended");
        put(511, "Network Authentication Required");
        put(598, "Network read timeout error");
        put(599, "Network connect timeout error");
    }};

    private static String encodeOp(int status) {
        if (!opcodes.containsKey(status)) return "500 Internal Server Error";

        var message = opcodes.get(status);
        return status + " " + message;
    }

    public final String op;
    public final byte[] body;
    public final String mime;
    public final Map<String, List<String>> headers;

    @SafeVarargs
    public Response(int op, byte[] body,
                     String mime, Map.Entry<String, List<String>>... headers) {
        this.op = encodeOp(op);
        this.body = body;
        this.mime = mime;
        this.headers = Map.ofEntries(headers);
    }

    @SafeVarargs
    public Response(int op, String body,
                    String mime, Map.Entry<String, List<String>>... headers) {
        this(op, body.getBytes(StandardCharsets.UTF_8), mime, headers);
    }
}