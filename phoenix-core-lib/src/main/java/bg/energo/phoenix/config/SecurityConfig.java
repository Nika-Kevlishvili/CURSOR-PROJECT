package bg.energo.phoenix.config;

import bg.energo.phoenix.security.AuthenticationFilter;
import bg.energo.phoenix.security.jwt.JwtVerifier;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        prePostEnabled = true,
        securedEnabled = true,
        jsr250Enabled = true)
public class SecurityConfig {

    @Autowired
    private JwtVerifier jwtVerifier;

    @Bean
    @ConditionalOnExpression("${app.cfg.authorization.enabled:true}")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests((authz) -> authz
                        .anyRequest().authenticated()
                )
                .httpBasic(withDefaults())
                .addFilterBefore(new AuthenticationFilter(jwtVerifier), BasicAuthenticationFilter.class);
        return http.build();
    }


    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers(HttpMethod.OPTIONS,"/**")//allow CORS option calls
//                .regexMatchers("^((?!permissions).)*$");
                .requestMatchers(
                "/documents/**",
                        "/actuator/**",
                        "/portal/**",
                        "/ignore2",
                        "/ws/**",
                        "/wss",
                        "/socket",
                        "/index.html",
                        "/topic",
                        "/app",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/v2/api-docs/**",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/epay/**"
                );
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearer-token",
                                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

}

