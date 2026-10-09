package bg.energo.phoenix.bulgariapost.auth.config;

import bg.energo.phoenix.bulgariapost.auth.filter.BulgariaPostAuthorizationFilter;
import bg.energo.phoenix.bulgariapost.auth.service.BulgariaPostAuthService;
import bg.energo.phoenix.bulgariapost.auth.service.BulgariaPostTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(BulgariaPostAuthProperties.class)
@ConditionalOnProperty(prefix = "bulgariapost.auth", name = "enabled", havingValue = "true")
public class BulgariaPostAuthConfiguration {

    @Bean
    public BulgariaPostTokenService bulgariaPostTokenService(
            StringRedisTemplate stringRedisTemplate,
            BulgariaPostAuthProperties properties
    ) {
        return new BulgariaPostTokenService(stringRedisTemplate, properties);
    }

    @Bean
    public BulgariaPostAuthService bulgariaPostAuthService(
            BulgariaPostAuthProperties properties,
            BulgariaPostTokenService tokenService
    ) {
        return new BulgariaPostAuthService(properties, tokenService);
    }

    @Bean
    public FilterRegistrationBean<BulgariaPostAuthorizationFilter> bulgariaPostAuthorizationFilter(
            BulgariaPostAuthProperties properties,
            BulgariaPostTokenService tokenService,
            ObjectMapper objectMapper
    ) {
        FilterRegistrationBean<BulgariaPostAuthorizationFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new BulgariaPostAuthorizationFilter(properties, tokenService, objectMapper));
        bean.addUrlPatterns(properties.getPathPrefix() + "/*");
        bean.setOrder(10);
        return bean;
    }

    @Bean
    @Order(0)
    public SecurityFilterChain bulgariaPostSecurityFilterChain(HttpSecurity http, BulgariaPostAuthProperties properties) throws Exception {
        http
                .securityMatcher(properties.getPathPrefix() + "/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}

