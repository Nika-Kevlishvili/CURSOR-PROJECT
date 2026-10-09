package bg.energo.phoenix_mass_import.interceptor;

import bg.energo.common.backend.jwt.PortalPrincipal;
import bg.energo.phoenix.security.jwt.JwtVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebsocketAuthInterceptor implements ChannelInterceptor {

    private final JwtVerifier jwtVerifier;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");

            if (token != null && token.startsWith("Bearer ")) {
                try {
                    String jwtToken = token.substring(7).trim();

                    jwtToken = jwtToken.replaceAll("\\s+", "");

                    PortalPrincipal portalPrincipal = jwtVerifier.verifyToken(jwtToken);

                    accessor.setUser(portalPrincipal);

                    log.info("WebSocket authenticated for user: {}", portalPrincipal.getName());

                } catch (Exception e) {
                    log.error("Exception caught during authorization: {}", e.getMessage());
                    throw new SecurityException("Invalid authentication");
                }
            } else {
                log.error("No valid authorization header found");
                throw new SecurityException("No authentication token provided");
            }
        }

        return message;
    }
}
