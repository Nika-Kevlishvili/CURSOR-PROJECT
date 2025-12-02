package bg.energo.phoenix.service.xEnergie;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;

@Slf4j
@Service
public class XEnergieDatabaseConnectionService {
    @Value("${xEnergie.database.connection-string}")
    private String oracleDatabaseConnectionString;

    @Value("${xEnergie.database.username}")
    private String oracleDatabaseUsername;

    @Value("${xEnergie.database.password}")
    private String oracleDatabasePassword;

    @Value("${xEnergie.database.connection-timeout}")
    private Integer connectionTimeoutInSeconds;

    public Connection openConnection() throws Exception {
        try {
            DriverManager.setLoginTimeout(connectionTimeoutInSeconds);
            return DriverManager.getConnection(oracleDatabaseConnectionString, oracleDatabaseUsername, oracleDatabasePassword);
        } catch (Exception e) {
            log.error("Error while trying to open connection with xEnergie database", e);
            throw e;
        }
    }
}
