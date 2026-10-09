//package bg.energo.phoenix_mass_import.config;
//
//import bg.energo.common.security.exception.PortalSecurityException;
//import bg.energo.phoenix.security.jwt.JwtVerifier;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Profile;
//import org.springframework.core.env.Environment;
//import org.springframework.http.server.ServerHttpRequest;
//import org.springframework.messaging.simp.config.MessageBrokerRegistry;
//import org.springframework.web.socket.WebSocketHandler;
//import org.springframework.web.socket.config.annotation.EnableWebSocket;
//import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
//import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
//import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
//import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
//
//import java.security.Principal;
//import java.util.Map;
//import java.util.Optional;
//
//@Configuration
//@EnableWebSocketMessageBroker
//@EnableWebSocket
//@RequiredArgsConstructor
////@Profile({"!dev","!test"})
//public class TestSocketConfig implements WebSocketMessageBrokerConfigurer {
//    private final JwtVerifier jwtVerifier;
//    private final Environment env;
//
//    @Override
//    public void configureMessageBroker(MessageBrokerRegistry config) {
//        config.enableSimpleBroker("/topic");
//        config.setApplicationDestinationPrefixes("/app");
//    }
//
//    @Override
//    public void registerStompEndpoints(StompEndpointRegistry registry) {
//        registry.addEndpoint("/ws")
//                .setAllowedOriginPatterns("*");
//
//        registry.addEndpoint("/ws")
//                .setAllowedOriginPatterns("*")
//                .setHandshakeHandler(new DefaultHandshakeHandler() {
//                    @Override
//                    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
//                        final Optional<String> authorization =
//                                Optional.ofNullable(request.getHeaders().getFirst("Authorization"));
//                        final Optional<String> deviceId =
//                                Optional.ofNullable(request.getHeaders().getFirst("Device-Id"));
//                        String tokenValue = authorization.get();
//                        String token = tokenValue.substring("Bearer".length()).trim();
//                        try {
//                            return jwtVerifier.verifyToken(token);
//                        } catch (PortalSecurityException e) {
//                            return super.determineUser(request, wsHandler, attributes);
//                        }
//                    }
//                }).withSockJS();
//
//    }
//}