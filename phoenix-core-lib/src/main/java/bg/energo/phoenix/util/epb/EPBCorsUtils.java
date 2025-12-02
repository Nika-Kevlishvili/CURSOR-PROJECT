package bg.energo.phoenix.util.epb;

import jakarta.servlet.http.HttpServletResponse;

public class EPBCorsUtils {

    public static void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Credentials","yes");
        response.setHeader("Access-Control-Allow-Headers","authorization");
        response.setHeader("Access-Control-Allow-Methods","*");
        response.setHeader("Access-Control-Allow-Origin","*");
        response.setHeader("Access-Control-Expose-Headers","Set-Cookie");
    }

}
