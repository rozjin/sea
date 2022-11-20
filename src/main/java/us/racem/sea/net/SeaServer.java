package us.racem.sea.net;

import rawhttp.core.RawHttp;
import rawhttp.core.RawHttpResponse;
import rawhttp.core.body.BytesBody;
import us.racem.sea.body.Response;
import us.racem.sea.fish.Ocean;
import us.racem.sea.fish.OceanExecutable;
import us.racem.sea.mark.methods.RequestMethod;
import us.racem.sea.route.RouteRegistry;
import us.racem.sea.util.InterpolationLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

public class SeaServer extends OceanExecutable {
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    private static final InterpolationLogger logger = InterpolationLogger.getLogger(Ocean.class);
    private static final String logPrefix = "SRV";
    private static final String headerSeperator = "\r\n";

    private int port;

    private int max_header_size;
    private int max_body_size;

    private RawHttp parser;

    private ServerSocket srv;

    public SeaServer(int port, int max_body_size, int max_header_size) {
        try {
            this.port = port;
            this.max_body_size = max_body_size;
            this.max_header_size = max_header_size;

            this.srv = new ServerSocket(port);
            this.parser = new RawHttp();
        } catch (IOException err) {
            logger.warn("%rFailed to initialize HTTP Server: {}%", err);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                var cs = srv.accept();
                executor.submit(() -> read(cs));
            } catch (IOException err) {
                logger.warn("%rError in HTTP Server: {}%", err);
            }
        }
    }

    private boolean hasBody(RequestMethod method) {
        return switch (method) {
            case POST, PUT, PATCH -> true;
            default -> false;
        };
    }

    private void read(Socket cs) {
        try (var is = cs.getInputStream();
             var os = cs.getOutputStream()) {
            var reqSize = is.available();
            if (reqSize > max_body_size + max_header_size) throw new IOException("Request too large!");

            var req = parser.parseRequest(is);

            var path = req.getUri()
                    .getPath()
                    .strip()
                    .replaceFirst("^/{1,2}", "");
            var method = RequestMethod.of(req.getMethod());
            var headers = req.getHeaders().asMap();

            logger.info("%bRequest {}%", "/" + path);

            byte[] body = null;
            if (hasBody(method) && req.getBody().isPresent()) {
                var bodyReader = req.getBody().get();
                if (bodyReader.getLengthIfKnown().isPresent() &&
                    bodyReader.getLengthIfKnown().getAsLong() > max_body_size) {
                    throw new IOException("Body too large!");
                }

                if (is.available() > max_body_size) {
                    throw new IOException("Body too large!");
                }

                body = req.getBody().get().decodeBody();
            }

            var res = RouteRegistry.requestOf(path, method, headers, body);
            var rsp = encodeResponse(res);
            rsp.writeTo(os);

            cs.close();
        } catch (Exception err) {
            logger.warn("%rUnable to Serve: {}%", err);
        }
    }

    private RawHttpResponse<Void> encodeResponse(Response rsp) {
        var headers = encodeResponseHeaders(rsp);
        if (rsp.body != null) {
            return parser
                    .parseResponse(headers)
                    .withBody(new BytesBody(rsp.body));
        }

        return parser.parseResponse(headers);
    }

    private String encodeResponseHeaders(Response rsp) {
        var date = RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));;
        var sb = new StringBuilder();
        var body = (byte[]) rsp.body;

        sb.append("HTTP/1.1 ")
          .append(rsp.op)
          .append(headerSeperator);

        sb.append("Content-Type: ")
          .append(rsp.mime)
          .append(headerSeperator);

        sb.append("Content-Length: ")
          .append(body == null ? 0 : body.length)
          .append(headerSeperator);

        sb.append("Date: ")
                .append(date)
                .append(headerSeperator);

        return sb.toString();
    }
}
