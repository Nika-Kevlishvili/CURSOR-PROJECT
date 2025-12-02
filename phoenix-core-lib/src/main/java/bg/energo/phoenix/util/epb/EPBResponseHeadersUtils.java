package bg.energo.phoenix.util.epb;

import org.springframework.http.HttpHeaders;

import java.util.Map;

public class EPBResponseHeadersUtils {

    public static HttpHeaders buildHeadersFromStringMap(Map<String, String> headersMap) {
        HttpHeaders httpHeaders = new HttpHeaders();
        headersMap.forEach(httpHeaders::add);
        return httpHeaders;
    }

}
