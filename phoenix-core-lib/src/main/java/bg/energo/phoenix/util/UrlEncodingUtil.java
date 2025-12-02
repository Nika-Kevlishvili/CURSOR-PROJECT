package bg.energo.phoenix.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UrlEncodingUtil {
    public static String encodeFileName(String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
