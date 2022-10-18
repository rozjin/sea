package us.racem.sea.body;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Response {
    public enum StatusCodes {
        $200 ("OK"),
        $201 ("Created"),
        $202 ("Accepted"),
        $203 ("Non-Authoritative Information"),
        $204 ("No Content"),
        $205 ("Reset Content"),
        $206 ("Partial Content"),
        $207 ("Multi-Status (WebDAV)"),
        $208 ("Already Reported (WebDAV)"),
        $226 ("IM Used"),
        $300 ("Multiple Choices"),
        $301 ("Moved Permanently"),
        $302 ("Found"),
        $303 ("See Other"),
        $304 ("Not Modified"),
        $305 ("Use Proxy"),
        $306 ("(Unused)"),
        $307 ("Temporary Redirect"),
        $308 ("Permanent Redirect (experimental)"),
        $400 ("Bad Request"),
        $401 ("Unauthorized"),
        $402 ("Payment Required"),
        $403 ("Forbidden"),
        $404 ("Not Found"),
        $405 ("Method Not Allowed"),
        $406 ("Not Acceptable"),
        $407 ("Proxy Authentication Required"),
        $408 ("Request Timeout"),
        $409 ("Conflict"),
        $410 ("Gone"),
        $411 ("Length Required"),
        $412 ("Precondition Failed"),
        $413 ("Request Entity Too Large"),
        $414 ("Request-URI Too Long"),
        $415 ("Unsupported Media Type"),
        $416 ("Requested Range Not Satisfiable"),
        $417 ("Expectation Failed"),
        $418 ("I'm a teapot (RFC 2324)"),
        $420 ("Enhance Your Calm (Twitter)"),
        $422 ("Unprocessable Entity (WebDAV)"),
        $423 ("Locked (WebDAV)"),
        $424 ("Failed Dependency (WebDAV)"),
        $425 ("Reserved for WebDAV"),
        $426 ("Upgrade Required"),
        $428 ("Precondition Required"),
        $429 ("Too Many Requests"),
        $431 ("Request Header Fields Too Large"),
        $444 ("No Response (Nginx)"),
        $449 ("Retry With (Microsoft)"),
        $450 ("Blocked by Windows Parental Controls (Microsoft)"),
        $451 ("Unavailable For Legal Reasons"),
        $499 ("Client Closed Request (Nginx)"),
        $500 ("Internal Server Error"),
        $501 ("Not Implemented"),
        $502 ("Bad Gateway"),
        $503 ("Service Unavailable"),
        $504 ("Gateway Timeout"),
        $505 ("HTTP Version Not Supported"),
        $506 ("Variant Also Negotiates (Experimental)"),
        $507 ("Insufficient Storage (WebDAV)"),
        $508 ("Loop Detected (WebDAV)"),
        $509 ("Bandwidth Limit Exceeded (Apache)"),
        $510 ("Not Extended"),
        $511 ("Network Authentication Required"),
        $598 ("Network read timeout error"),
        $599 ("Network connect timeout error");

        public final String msg;
        StatusCodes(String msg) {
            this.msg = msg;
        }

        public String encode() {
            return this.toString().substring(1) + " " + msg;
        }
    }

    private static final List<Integer> validOpcodes = List.of(200, 201, 202, 203, 204, 205, 206, 207, 208, 226, 300, 301, 302, 303, 304, 305, 306, 307, 308, 400, 401, 402, 403, 404, 405, 406, 407, 408, 409, 410, 411, 412, 413, 414, 415, 416, 417, 418, 420, 422, 423, 424, 425, 426, 428, 429, 431, 444, 449, 450, 451, 499, 500, 501, 502, 503, 504, 505, 506, 507, 508, 509, 510, 511, 598, 599);

    public final StatusCodes status;
    public final Object body;
    public final String mime;
    public final Map<String, List<String>> headers;

    private Response(StatusCodes status, byte[] body,
                     String mime, Map<String, List<String>> headers) {
        this.status = status;
        this.body = body;
        this.mime = mime;
        this.headers = headers;
    }

    @SafeVarargs
    public static Response of(int status, byte[] body,
                              String mime, Map.Entry<String, List<String>>... headers) {
        if (validOpcodes.contains(status)) return new Response(
                StatusCodes.valueOf("$" + status),
                body,
                mime,
                Map.ofEntries(headers));

        return new Response(StatusCodes.$500, body, mime, Map.ofEntries(headers));
    }

    @SafeVarargs
    public static Response of(int status, String body,
                              String mime, Map.Entry<String, List<String>>... headers) {
        return of(status, body.getBytes(StandardCharsets.UTF_8), mime, headers);
    }


    public static Response from(StatusCodes status, byte[] body,
                                String mime, Map<String, List<String>> headers) {
        return new Response(status, body, mime, headers);
    }

    @Override
    public String toString() {
        return "Response{" +
                "status=" + status +
                ", body=" + body +
                ", mime='" + mime + '\'' +
                ", headers=" + headers +
                '}';
    }
}