package bg.energo.phoenix.systech.auth.config;

import bg.energo.phoenix.systech.auth.filter.SystechAuthorizationFilter;
import bg.energo.phoenix.systech.auth.service.SystechAuthService;
import bg.energo.phoenix.systech.auth.service.SystechTokenService;
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
@EnableConfigurationProperties(SystechAuthProperties.class)
@ConditionalOnProperty(prefix = "systech.auth", name = "enabled", havingValue = "true")
public class SystechAuthConfiguration {

    @Bean
    public SystechTokenService systechTokenService(
            StringRedisTemplate stringRedisTemplate,
            SystechAuthProperties properties
    ) {
        return new SystechTokenService(stringRedisTemplate, properties);
    }

    @Bean
    public SystechAuthService systechAuthService(
            SystechAuthProperties properties,
            SystechTokenService tokenService
    ) {
        return new SystechAuthService(properties, tokenService);
    }

    @Bean
    public FilterRegistrationBean<SystechAuthorizationFilter> systechAuthorizationFilter(
            SystechAuthProperties properties,
            SystechTokenService tokenService,
            ObjectMapper objectMapper
    ) {
        FilterRegistrationBean<SystechAuthorizationFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new SystechAuthorizationFilter(properties, tokenService, objectMapper));
        bean.addUrlPatterns(properties.getPathPrefix() + "/*");
        bean.setOrder(11);
        return bean;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain systechSecurityFilterChain(HttpSecurity http, SystechAuthProperties properties) throws Exception {
        http
                .securityMatcher(properties.getPathPrefix() + "/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
