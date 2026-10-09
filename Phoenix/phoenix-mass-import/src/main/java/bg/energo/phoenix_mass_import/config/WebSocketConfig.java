package bg.energo.phoenix_mass_import.config;


import bg.energo.phoenix.security.jwt.JwtVerifier;
import bg.energo.phoenix_mass_import.interceptor.WebsocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@EnableWebSocket
@RequiredArgsConstructor
@Profile({"dev", "test","preProd", "prod"})
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtVerifier jwtVerifier;
    private final Environment env;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableStompBrokerRelay("/queue", "/topic")
                .setRelayHost(env.getRequiredProperty("spring.rabbitmq.host"))
                .setRelayPort(env.getRequiredProperty("rabbit.stomp.port", Integer.class))
                .setClientPasscode(env.getRequiredProperty("spring.rabbitmq.password"))
                .setClientLogin(env.getRequiredProperty("spring.rabbitmq.username"))
                .setSystemLogin(env.getRequiredProperty("spring.rabbitmq.username"))
                .setSystemPasscode(env.getRequiredProperty("spring.rabbitmq.password"));
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*").withSockJS();

    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new WebsocketAuthInterceptor(jwtVerifier));
    }
}