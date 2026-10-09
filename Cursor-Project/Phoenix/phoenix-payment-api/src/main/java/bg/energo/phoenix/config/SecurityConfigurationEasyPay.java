package bg.energo.phoenix.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(
        prePostEnabled = true,
        securedEnabled = true,
        jsr250Enabled = true
)
public class SecurityConfigurationEasyPay extends GlobalMethodSecurityConfiguration {


//    @Bean
//    @ConditionalOnExpression("${app.cfg.authorization.enabled:true}")
//    public SecurityFilterChain filterChainEasyPay(HttpSecurity http) throws Exception {
//        http
//                .cors(AbstractHttpConfigurer::disable)
//                .csrf(AbstractHttpConfigurer::disable)
//                .authorizeHttpRequests((authz) -> authz
//                                .requestMatchers(
//                                        "/swagger-ui.html",
//                                        "/swagger-ui/**",
//                                        "/v3/api-docs/**",
//                                        "/v2/api-docs/**",
//                                        "/webjars/**",
//                                        "/test/**",                 //TODO: remove
//                                        "/swagger-resources/**",
//                                        "/epay-test/pay/init",    //TODO: remove
//                                        "/epay-test/pay/confirm" //TODO: remove
//                                )
//                                .permitAll()
////TODO : Add token verification with permissions
//                );
//        return http.build();
//    }


//    @Bean
//    public OpenAPI customOpenAPIEasyPay() {
//        return new OpenAPI()
//                .components(new Components()
//                        .addSecuritySchemes("bearer-token",
//                                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
//    }


}

