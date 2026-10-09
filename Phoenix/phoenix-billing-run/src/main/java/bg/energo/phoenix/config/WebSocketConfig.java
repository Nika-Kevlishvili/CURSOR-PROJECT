package bg.energo.phoenix.config;


import bg.energo.common.portal.api.user.PortalUserForApplicationDto;
import bg.energo.common.security.exception.PortalSecurityException;
import bg.energo.phoenix.model.principal.EnergoProPrincipal;
import bg.energo.phoenix.security.jwt.JwtVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.AbstractHandshakeHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@EnableWebSocket
@RequiredArgsConstructor
@Profile({"dev", "test","preProd","prod"})
@Slf4j

public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtVerifier jwtVerifier;
    private final Environment env;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {

        config.enableStompBrokerRelay("/topic", "/queue", "/exchange")
                .setRelayHost(env.getRequiredProperty("spring.rabbitmq.host"))
                .setRelayPort(env.getRequiredProperty("rabbit.stomp.port", Integer.class))
                .setClientPasscode(env.getRequiredProperty("spring.rabbitmq.password"))
                .setClientLogin(env.getRequiredProperty("spring.rabbitmq.username"))
                .setSystemLogin(env.getRequiredProperty("spring.rabbitmq.username"))
                .setSystemPasscode(env.getRequiredProperty("spring.rabbitmq.password"))
                .setUserDestinationBroadcast("/topic/unresolved-user-destinations")
                .setUserRegistryBroadcast("/topic/user-registry");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new AbstractHandshakeHandler() {

                    @Override
                    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {

                        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
                        String token = queryParams.getFirst("token");

                        if (token != null) {
                            log.info("Token received: {}", token.substring(0, 10) + "...");
                            try {
                                PortalUserForApplicationDto portalPrincipal = jwtVerifier.verifyTokenAndGetApplicationUser(token);
                                attributes.put("user", portalPrincipal);
                                return new EnergoProPrincipal(token,portalPrincipal);
                            } catch (PortalSecurityException e) {
                                throw new RuntimeException(e);
                            }

                        }
                        return super.determineUser(request, wsHandler, attributes);
                    }
                })
                .withSockJS();
    }
}