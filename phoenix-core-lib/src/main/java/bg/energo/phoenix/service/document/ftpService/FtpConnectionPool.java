package bg.energo.phoenix.service.document.ftpService;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Manages a pool of FTP connections for the application.
 * The pool is configured with various settings such as maximum total connections,
 * minimum and maximum idle connections, and connection timeout.
 * The pool is used to efficiently manage and reuse FTP connections,
 * reducing the overhead of creating new connections for each request.
 */
@Service
@Profile({"dev", "test", "local"})
//@ConditionalOnExpression("${app.cfg.ftp.enabled:true}")
public class FtpConnectionPool {

    private final GenericObjectPool<FtpConnection> pool;

    public FtpConnectionPool(
            Environment env
    ) {

        Integer ftpMaxTotal = env.getRequiredProperty("ftp.server.maxTotal", Integer.class);
        Integer ftpMinIdle = env.getRequiredProperty("ftp.server.minIdle", Integer.class);
        Integer ftpMaxIdle = env.getRequiredProperty("ftp.server.maxIdle", Integer.class);
        Integer timeBetweenEvictionRuns = env.getRequiredProperty("ftp.server.timeBetweenEvictionRuns", Integer.class);

        GenericObjectPoolConfig<FtpConnection> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(ftpMaxTotal); // Maximum number of objects in the pool
        config.setMinIdle(ftpMinIdle); // Minimum number of idle objects
        config.setMaxIdle(ftpMaxIdle); // Maximum number of idle objects
        config.setBlockWhenExhausted(true); // Block when pool is exhausted
        config.setFairness(true);
        config.setTestWhileIdle(true);
        config.setTimeBetweenEvictionRuns(Duration.ofMinutes(timeBetweenEvictionRuns));
        config.setMaxWait(Duration.ofMillis(env.getRequiredProperty("ftp.server.maxWait", Integer.class)));
        String username = env.getRequiredProperty("ftp.server.username", String.class);
        String password = env.getRequiredProperty("ftp.server.password", String.class);
        String ftpHost = env.getRequiredProperty("ftp.server.host", String.class);
        Integer ftpPort = env.getRequiredProperty("ftp.server.port", Integer.class);
        Integer connectionTimeout = env.getRequiredProperty("ftp.server.connection-timeout.millis", Integer.class);
        FtpConnectionFactory ftpConnectionFactory = new FtpConnectionFactory(connectionTimeout, ftpHost, ftpPort, username, password);
        this.pool = new GenericObjectPool<>(ftpConnectionFactory, config);

    }

    /**
     * Retrieves an FTP connection from the connection pool.
     * If no connection is available, a new one is created and returned.
     * If an exception occurs while borrowing the connection, a ClientException is thrown.
     *
     * @return an FTP connection from the connection pool
     * @throws ClientException if unable to acquire an FTP connection
     */
    public FtpConnection getConnection() {
        try {
            return pool.borrowObject();
        } catch (Exception e) {
            throw new ClientException("Unable to acquire ftp connection!;", ErrorCode.APPLICATION_ERROR);
        }
    }


    /**
     * Returns an FTP connection to the connection pool.
     *
     * @param ftpConnection the FTP connection to be returned to the pool
     */
    public void returnConnection(FtpConnection ftpConnection) {
        pool.returnObject(ftpConnection);
    }

    //Todo will be deleted;
    public GenericObjectPool<FtpConnection> getMyPool() {
        return pool;
    }

}
