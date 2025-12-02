package bg.energo.phoenix.security;

import bg.energo.common.portal.api.user.PortalUserForApplicationDto;
import bg.energo.common.security.acl.enums.AclUserStatus;
import bg.energo.phoenix.model.principal.EnergoProPrincipal;
import bg.energo.phoenix.security.jwt.JwtVerifier;
import bg.energo.phoenix.util.epb.EPBCorsUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.util.Optional;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
public class AuthenticationFilter extends GenericFilterBean {

    private final JwtVerifier jwtVerifier;

    public AuthenticationFilter(JwtVerifier jwtVerifier) {
        this.jwtVerifier = jwtVerifier;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException {
        log.debug("Filtering request");
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        final Optional<String> authorization =
                Optional.ofNullable(((HttpServletRequest) request).getHeader(AUTHORIZATION));
        if (authorization.isEmpty()) {
            log.debug("No bearer token found - return empty.");
            SecurityContextHolder.clearContext();
            EPBCorsUtils.setCorsHeaders(httpResponse);
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid access");
            return;
        }
        try {
            String tokenValue = authorization.get();
            String token = tokenValue.substring("Bearer".length()).trim();
            log.debug("Bearer token is {}", tokenValue);
            final PortalUserForApplicationDto applicationUser = jwtVerifier.verifyTokenAndGetApplicationUser(token);
            if (applicationUser != null && jwtVerifier.getUserStatus(applicationUser).equals(AclUserStatus.ACTIVE)) {
                SecurityContextHolder.getContext().setAuthentication(new EnergoProPrincipal(token, applicationUser));
                chain.doFilter(request, response);
            } else {
                log.error("Invalid portal user! ");
                SecurityContextHolder.clearContext();
                EPBCorsUtils.setCorsHeaders(httpResponse);
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid access");
            }
        } catch (Exception e) {
            log.error("Portal Security Exception", e);
            SecurityContextHolder.clearContext();
            EPBCorsUtils.setCorsHeaders(httpResponse);
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid access");
        }
    }
}
