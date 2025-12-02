package bg.energo.phoenix.service.document.ftpService;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Service
@Profile({"dev", "test", "local"})
public class FtpFileServiceNew implements FileService {
    private final FtpConnectionPool ftpConnectionPool;

    /**
     * Uploads a file to the FTP server.
     *
     * @param file       The file to be uploaded.
     * @param remotePath The remote path on the FTP server where the file should be uploaded.
     * @param fileName   The name of the file to be uploaded.
     * @return The path of the uploaded file on the FTP server.
     * @throws ClientException If there is an error while reading or uploading the file.
     */
    public String uploadFile(MultipartFile file, String remotePath, String fileName) {
        long startTime = System.currentTimeMillis();
        log.debug("Start time {}", startTime);
        FtpConnection connection = ftpConnectionPool.getConnection();
        log.debug("EndTime time {}", System.currentTimeMillis() - startTime);
        try (InputStream inputStream = file.getInputStream()) {
            return connection.uploadFile(remotePath, fileName, inputStream);
        } catch (IOException ex) {
            log.error("FtpError {}", ex.getMessage());
            ex.printStackTrace();
            throw new ClientException("Error while reading file!;", ErrorCode.APPLICATION_ERROR);
        } finally {
            ftpConnectionPool.returnConnection(connection);
        }
    }

    /**
     * Downloads a file from the FTP server.
     *
     * @param fileUrl The URL of the file to be downloaded.
     * @return A ByteArrayResource containing the contents of the downloaded file.
     * @throws ClientException If there is an error while downloading the file.
     */
    public ByteArrayResource downloadFile(String fileUrl) {
        Optional.ofNullable(fileUrl)
                .orElseThrow(() -> new IllegalArgumentsProvidedException("File url is null!"));

        FtpConnection connection = ftpConnectionPool.getConnection();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            boolean retrieved = connection.downloadFile(fileUrl, baos);
            if (!retrieved) {
                log.error("FtpClient: Could not retrieve file " + fileUrl);
                throw new ClientException("Could not retrieve file " + fileUrl + ". Please try again!", ErrorCode.APPLICATION_ERROR);
            }
            return new ByteArrayResource(baos.toByteArray());
        } catch (IOException ex) {
            log.error("FtpError {}", ex.getMessage());
            ex.printStackTrace();
            throw new ClientException("Error while reading file!;", ErrorCode.APPLICATION_ERROR);
        } finally {
            ftpConnectionPool.returnConnection(connection);
        }
    }

    /**
     * Uploads a file to the FTP server.
     *
     * @param file       The file to be uploaded.
     * @param remotePath The remote path on the FTP server where the file should be uploaded.
     * @param fileName   The name of the file to be uploaded.
     * @return The path of the uploaded file on the FTP server.
     * @throws ClientException If there is an error while reading or uploading the file.
     */
    public String uploadFile(File file, String remotePath, String fileName) {
        FtpConnection connection = ftpConnectionPool.getConnection();

        try (InputStream inputStream = new FileInputStream(file)) {
            return connection.uploadFile(remotePath, fileName, inputStream);
        } catch (IOException ex) {
            log.error("FtpError {}", ex.getMessage());
            ex.printStackTrace();
            throw new ClientException("Error while reading file!;", ErrorCode.APPLICATION_ERROR);
        } finally {
            ftpConnectionPool.returnConnection(connection);
        }
    }


    public List<MultipartFile> fetchFilesFromFolder(String folderPath) {
        FtpConnection connection = ftpConnectionPool.getConnection();
        try {
            return connection.fetchFilesFromFolder(folderPath);
        } catch (Exception e) {
            log.error("FtpError {}", e.getMessage());
            e.printStackTrace();
            throw new ClientException("Could not retrieve files from FTP server", ErrorCode.APPLICATION_ERROR);
        } finally {
            ftpConnectionPool.returnConnection(connection);
        }
    }

    /**
     * Deletes a file or directory on a remote FTP server given the remote path.
     *
     * @param remotePath the path to the file or directory on the remote FTP server
     * @throws ClientException if an error occurs while deleting the file or directory
     */
    public void deleteOnPath(String remotePath) {
        Optional.ofNullable(remotePath)
                .orElseThrow(() -> new IllegalArgumentsProvidedException("File url is null!"));

        FtpConnection connection = ftpConnectionPool.getConnection();
        try {
            connection.deleteOnPath(remotePath);
        } catch (Exception e) {
            log.error("FtpError {}", e.getMessage());
            log.error("Exception handled while listing folders on remotePath: %s".formatted(remotePath), e);
            throw new ClientException(e.getMessage(), ErrorCode.APPLICATION_ERROR);
        } finally {
            ftpConnectionPool.returnConnection(connection);
        }
    }
}
