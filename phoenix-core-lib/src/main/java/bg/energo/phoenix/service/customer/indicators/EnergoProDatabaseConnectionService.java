package bg.energo.phoenix.service.customer.indicators;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;

@Slf4j
@Service
public class EnergoProDatabaseConnectionService {
    @Value("${energopro.database.connection-string}")
    private String databaseConnectionString;

    @Value("${energopro.database.username}")
    private String databaseUsername;

    @Value("${energopro.database.password}")
    private String databasePassword;

    @Value("${energopro.database.connection-timeout}")
    private Integer connectionTimeoutInSeconds;

    public Connection openConnection() throws Exception {
        try {
            DriverManager.setLoginTimeout(connectionTimeoutInSeconds);
            return DriverManager.getConnection(databaseConnectionString, databaseUsername, databasePassword);
        } catch (Exception e) {
            log.error("Error while trying to open connection with EnergoPro database", e);
            throw e;
        }
    }
}
