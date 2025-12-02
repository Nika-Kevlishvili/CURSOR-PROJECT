package bg.energo.phoenix.service.document.ftpService;

import lombok.RequiredArgsConstructor;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
@RequiredArgsConstructor
public class FtpConnectionFactory extends BasePooledObjectFactory<FtpConnection> {

    private final int connectionTimeout;
    private final String ftpHost;
    private final int ftpPort;
    private final String username;
    private final String password;
    @Override
    public FtpConnection create() throws Exception {
        return new FtpConnection(connectionTimeout,ftpHost,ftpPort,username,password);
    }

    @Override
    public PooledObject<FtpConnection> wrap(FtpConnection myConnection) {
        return new DefaultPooledObject<>(myConnection);
    }

    @Override
    public void destroyObject(PooledObject<FtpConnection> pooledObject) throws Exception {
        pooledObject.getObject().disconnect();
    }


    @Override
    public boolean validateObject(PooledObject<FtpConnection> p) {
        return p.getObject().isConnected() ;
    }
}
