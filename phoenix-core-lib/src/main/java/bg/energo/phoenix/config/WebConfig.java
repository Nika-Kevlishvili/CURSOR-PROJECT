package bg.energo.phoenix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.WebJarsResourceResolver;
import org.webjars.WebJarAssetLocator;


@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new NomenclatureConverter());
    }

    @Bean
    public WebJarAssetLocator webJarAssetLocator() {
        return new WebJarAssetLocator("bg.energo.phoenix");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .resourceChain(true)
                .addResolver(new WebJarsResourceResolver(webJarAssetLocator()));
    }
}
