package us.racem.sea.util;

import com.google.common.net.InetAddresses;
import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class ArgumentParser {
    private static final String regexStr = "(?<DSH>[-]+)(?:\\s*)(?<ARG>[A-Za-z_.]+)(?:\\s*)(?<VAL>[A-Za-z0-9_+/=.]+)";
    private static final Pattern regexPtrn = Pattern.compile(regexStr);

    public static Map<String, String> parse(String[] arr) {
        var args = new HashMap<String, String>();

        var chrs = String.join(" ", arr);
        var regex = regexPtrn.matcher(chrs);
        while (regex.find()) {
            if (regex.start() == -1) {
                continue;
            }

            var dashes = regex.group("DSH").length();
            var arg = regex.group("ARG");
            var val = regex.group("VAL");

            args.put(arg, val);
        }

        return args;
    }

    public static String uuidToBase64(UUID uuid) {
        var id = uuid.toString();
        return Base64.encodeBase64String(id.getBytes(StandardCharsets.UTF_8));
    }

    public static UUID uuidFromBase64(String id) {
        var bytes = Base64.decodeBase64(id);
        var base64 = new String(bytes, StandardCharsets.UTF_8);
        return UUID.fromString(base64);
    }

    public static UUID parseUUID(String id) {
        try {
            return uuidFromBase64(id);
        } catch (Exception ex) {
            return UUID.randomUUID();
        }
    }

    public static String parseIP(String rem) {
        try {
            if (!InetAddresses.isInetAddress(rem)) return null;
        } catch (Exception err) {
            return null;
        }

        return rem;
    }

    public static int parseInt(String port) {
        if (port == null) return -1;
        return Integer.parseInt(port);
    }
}
