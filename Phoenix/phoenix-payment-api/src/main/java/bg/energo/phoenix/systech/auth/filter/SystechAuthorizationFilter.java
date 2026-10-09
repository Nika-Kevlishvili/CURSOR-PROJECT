package bg.energo.phoenix.systech.auth.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import bg.energo.phoenix.systech.auth.SystechAuthConstants;
import bg.energo.phoenix.systech.auth.config.SystechAuthProperties;
import bg.energo.phoenix.systech.auth.service.SystechTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class SystechAuthorizationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SystechAuthorizationFilter.class);

    private final SystechAuthProperties properties;
    private final SystechTokenService tokenService;
    private final ObjectMapper objectMapper;

    public SystechAuthorizationFilter(
            SystechAuthProperties properties,
            SystechTokenService tokenService,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        final String prefix = properties.getPathPrefix();
        if (prefix == null || prefix.isBlank()) {
            return true;
        }
        final String path = request.getServletPath();
        if (path == null || !path.startsWith(prefix)) {
            return true;
        }
        if (path.equals(prefix + "/auth")) {
            return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String token = extractBearer(authHeader);
        if (token == null) {
            writeUnauthorizedJson(response);
            return;
        }

        boolean ok;
        try {
            ok = tokenService.consume(token);
        } catch (Exception e) {
            log.warn("Systech token consume failed (will reject). path={}", request.getRequestURI(), e);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Authorization backend unavailable");
            return;
        }

        if (!ok) {
            writeUnauthorizedJson(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorizedJson(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ErrorCode", SystechAuthConstants.UNAUTHORIZED_ERROR_CODE);
        body.put("ErrorMessage", SystechAuthConstants.UNAUTHORIZED_ERROR_MESSAGE);
        body.put("Results", List.of());

        objectMapper.writeValue(response.getWriter(), body);
    }

    private static String extractBearer(String authorizationHeader) {
        if (authorizationHeader == null) {
            return null;
        }
        final String value = authorizationHeader.trim();
        if (value.length() < 8) { // "Bearer x"
            return null;
        }
        if (!value.regionMatches(true, 0, "Bearer", 0, "Bearer".length())) {
            return null;
        }
        final String token = value.substring("Bearer".length()).trim();
        return token.isEmpty() ? null : token;
    }
}
