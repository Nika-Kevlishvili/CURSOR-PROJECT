package bg.energo.phoenix.config;

import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class DataSourceProxyConfiguration {
    @Bean
    public DataSource getDataSource(DataSourceProperties dataSourceProperties) {
        DataSource originalDatasource = dataSourceProperties.initializeDataSourceBuilder()
                .build();
        return ProxyDataSourceBuilder.create(originalDatasource)
                .name("Proxy DataSource")
                .logQueryByCommons(log.getName())
                .build();
    }
}
