package bg.energo.phoenix.config;

import bg.energo.phoenix.service.CustomLoggerInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RequiredArgsConstructor
@Component
public class InterceptorConfig implements WebMvcConfigurer {
    private final CustomLoggerInterceptor interceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor);
    }
}
