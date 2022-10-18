package us.racem.sea.net;

import okio.Okio;
import rawhttp.core.RawHttp;
import rawhttp.core.RawHttpResponse;
import rawhttp.core.body.BytesBody;
import us.racem.sea.body.Response;
import us.racem.sea.fish.Ocean;
import us.racem.sea.fish.OceanExecutable;
import us.racem.sea.mark.methods.RequestMethod;
import us.racem.sea.route.Router;
import us.racem.sea.util.InterpolationLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

public class SeaServer extends OceanExecutable {
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private static final InterpolationLogger logger = InterpolationLogger.getLogger(Ocean.class);
    private static final String logPrefix = "SRV";
    private static final String headerSeperator = "\r\n";

    private int port;

    private int max_header_size = 16384;
    private int max_body_size = 1048576;

    private RawHttp parser;

    private ServerSocket srv;

    public SeaServer(int port) {
        try {
            this.port = port;
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
            if (req.getBody().isPresent()) {
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

            var res = Router.request(method, path, headers, body);
            var rsp = encodeResponse(res);
            rsp.writeTo(os);

            cs.close();
        } catch (IOException err) {
            logger.warn("%rUnable to Read: {}%", err);
        }
    }

    private RawHttpResponse<Void> encodeResponse(Response rsp) {
        var headers = encodeResponseHeaders(rsp);
        if (rsp.body != null) {
            return parser
                    .parseResponse(headers)
                    .withBody(new BytesBody((byte[]) rsp.body));
        }

        return parser.parseResponse(headers);
    }

    private String encodeResponseHeaders(Response rsp) {
        var date = RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));;
        var sb = new StringBuilder();
        var body = (byte[]) rsp.body;

        sb.append("HTTP/1.1 ")
          .append(rsp.status.encode())
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
