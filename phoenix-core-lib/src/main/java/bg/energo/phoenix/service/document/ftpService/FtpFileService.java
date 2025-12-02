package bg.energo.phoenix.service.document.ftpService;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.util.ByteMultiPartFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FtpFileService {


    @Value("${ftp.server.host}")
    private String ftpHost;

    @Value("${ftp.server.port}")
    private String ftpPort;

    @Value("${ftp.server.username}")
    private String ftpUsername;

    @Value("${ftp.server.password}")
    private String ftpPassword;

    @Value("${ftp.server.base.path}")
    private String ftpBasePath;

    @Value("${ftp.server.connection-timeout.millis}")
    private Integer connectionTimeout;


    /**
     * Uploads a file to the remote FTP server.
     *
     * @param file       the {@link MultipartFile} to be uploaded
     * @param remotePath the path on the FTP server where the file will be uploaded
     * @param fileName   the name of the file that will be stored on the FTP server
     * @return the full path of the uploaded file on the remote FTP server
     * @throws ClientException if the file could not be uploaded
     */

    public String uploadFile(MultipartFile file, String remotePath, String fileName) {
        fileName = fileName.replaceAll("\\s+", "");

        log.debug("FtpClient starting to upload file: {} to remote path: {}", fileName, remotePath);

        FTPClient ftpClient = new FTPClient();
        try (InputStream inputStream = file.getInputStream()) {

            log.debug("FtpClient: trying to connect");
            ftpClient.setConnectTimeout(connectionTimeout);
            ftpClient.connect(ftpHost, Integer.parseInt(ftpPort));

            boolean loggedIn = ftpClient.login(ftpUsername, ftpPassword);
            log.debug("FtpClient login success: " + loggedIn);

            ftpCreateDirectoryTree(ftpClient, remotePath);

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            String fullPath = remotePath + "/" + fileName;

            boolean stored = ftpClient.storeFile(fileName, inputStream);
            log.debug("File was stored: {}, Reply code is: {}, Reply string is: {}", stored, ftpClient.getReplyCode(), ftpClient.getReplyString());

            if (!stored) {
                log.error("FtpClient: Could not store file");
                throw new ClientException("Could not store file " + fileName + ". Please try again!", ErrorCode.APPLICATION_ERROR);
            }

            log.debug("Stored file full path: " + fullPath);
            return fullPath;
        } catch (IOException ex) {
            log.error("FtpClient: Could not store file: %s, Reply code is: %s, Reply string is: %s".formatted(fileName, ftpClient.getReplyCode(), ftpClient.getReplyString()));
            throw new ClientException("Could not store file %s. Please try again! \n %s".formatted(fileName, ex.getMessage()), ErrorCode.APPLICATION_ERROR);
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                log.error("Exception while trying to disconnect ftp client", ex);
            }
        }
    }


    public String uploadFile(File file, String remotePath, String fileName) {
        fileName = fileName.replaceAll("\\s+", "");

        log.debug("FtpClient starting to upload file: {} to remote path: {}", fileName, remotePath);
        long currentTimeMillis = System.currentTimeMillis();
        FTPClient ftpClient = new FTPClient();
        try (InputStream inputStream = new FileInputStream(file)) {
            log.debug("FtpClient: trying to connect");
            ftpClient.setConnectTimeout(connectionTimeout);
            ftpClient.connect(ftpHost, Integer.parseInt(ftpPort));
            log.debug("FtpClient: connected successfully");

            log.debug("FtpClient: trying to log in");
            boolean loggedIn = ftpClient.login(ftpUsername, ftpPassword);
            log.debug("FtpClient: login success: " + loggedIn);

            ftpCreateDirectoryTree(ftpClient, remotePath);

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            String fullPath = remotePath + "/" + fileName;
            log.debug("Full path for storing file is: [%s]".formatted(fullPath));

            log.debug("Trying to store file: [%s]".formatted(fileName));
            log.debug("Current working directory is: [%s]".formatted(ftpClient.printWorkingDirectory()));
            log.debug("FtpTime took {} to connect;", System.currentTimeMillis() - currentTimeMillis);
            boolean stored = ftpClient.storeFile(fileName, inputStream);
            log.debug("File was stored: {}, Reply code is: {}, Reply string is: {}", stored, ftpClient.getReplyCode(), ftpClient.getReplyString());
            log.debug("FtpTime took {} to store;", System.currentTimeMillis() - currentTimeMillis);
            if (!stored) {
                log.error("FtpClient: Could not store file");
                throw new ClientException("Could not store file " + fileName + ". Please try again!", ErrorCode.APPLICATION_ERROR);
            }

            log.debug("Stored file full path: " + fullPath);
            return fullPath;
        } catch (IOException ex) {
            log.error("FtpClient: Could not store file: %s, Reply Code is: [%s], Reply String is: [%s]".formatted(fileName, ftpClient.getReplyCode(), ftpClient.getReplyString()), ex);
            throw new ClientException("Could not store file %s. Please try again! \n %s".formatted(fileName, ex.getMessage()), ErrorCode.APPLICATION_ERROR);
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                log.error("Exception while trying to disconnect ftp client", ex);
            }
        }
    }

    /**
     * Deletes a file or directory on a remote FTP server given the remote path.
     *
     * @param remotePath the path to the file or directory on the remote FTP server
     * @throws ClientException if an error occurs while deleting the file or directory
     */

    public void deleteOnPath(String remotePath) {
        FTPClient ftpClient = new FTPClient();
        try {
            log.debug("Before ftp connect");
            ftpClient.connect(ftpHost, Integer.parseInt(ftpPort));
            log.debug("before ftp login");

            ftpClient.login(ftpUsername, ftpPassword);
            ftpClient.enterLocalPassiveMode();

            String[] folderPaths = remotePath.split("/");
            int subPathsCount = folderPaths.length;
            String lastElement = folderPaths[subPathsCount - 1];
            if (lastElement.contains(".")) {
                for (int i = 0; i < subPathsCount - 1; i++) {
                    String folderPath = folderPaths[0];
                    ftpClient.changeWorkingDirectory(folderPath);
                }
            } else {
                for (String folderPath : folderPaths) {
                    ftpClient.changeWorkingDirectory(folderPath);
                }
            }

            try {
                ftpClient.dele(lastElement);
            } catch (Exception e) {
                log.error("Delete directory failed");
                throw new ClientException("Delete directory failed: %s".formatted(e.getMessage()), ErrorCode.APPLICATION_ERROR);
            }

            try {
                ftpClient.deleteFile(lastElement);
            } catch (Exception e) {
                log.error("Delete file failed");
                throw new ClientException("Delete file failed: %s".formatted(e.getMessage()), ErrorCode.APPLICATION_ERROR);
            }
        } catch (Exception e) {
            log.error("Exception handled while listing folders on remotePath: %s".formatted(remotePath), e);
            throw new ClientException(e.getMessage(), ErrorCode.APPLICATION_ERROR);
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                log.error("Exception while trying to disconnect ftp client", ex);
            }
        }
    }


    public List<MultipartFile> fetchFilesFromFolder(String folderPath) {
        log.debug("FtpClient starting to fetch files from folder: " + folderPath);
        List<MultipartFile> files = new ArrayList<>();
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.setConnectTimeout(connectionTimeout);
            ftpClient.connect(ftpHost, Integer.parseInt(ftpPort));
            boolean loggedIn = ftpClient.login(ftpUsername, ftpPassword);

            log.debug("FtpClient login success: " + loggedIn);

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            FTPFile[] ftpFiles = ftpClient.listFiles(folderPath);

            for (FTPFile file : ftpFiles) {
                if (file.isFile()) {
                    String fileName = file.getName();
                    InputStream inputStream = ftpClient.retrieveFileStream(folderPath + "/" + fileName);

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    byte[] fileContent = outputStream.toByteArray();

                    MultipartFile multiPartFile = constructMultipartFile(file, fileContent);
                    files.add(multiPartFile);
                    inputStream.close();
                    ftpClient.completePendingCommand();
                }
            }
            return files;
        } catch (Exception e) {
            log.debug(e.getMessage());
            throw new ClientException("Could not retrieve files from FTP server", ErrorCode.APPLICATION_ERROR);
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                log.error("Exception while trying to disconnect ftp client", ex);
            }
        }
    }

    private MultipartFile constructMultipartFile(FTPFile file, byte[] content) {
        if (file.getName().endsWith(".txt")) {
            return new ByteMultiPartFile(file.getName(), content, "text/plain");
        }
        return new ByteMultiPartFile(file.getName(), content);
    }

    /**
     * Downloads a file from a FTP server and returns it as a {@link ByteArrayResource}.
     *
     * @param fileUrl the path of the file to be downloaded.
     * @return a {@link ByteArrayResource} representing the downloaded file.
     * @throws ClientException if there is an error while downloading the file.
     */

    public ByteArrayResource downloadFile(String fileUrl) {
        log.debug("FtpClient starting to download file from path: " + fileUrl);

        FTPClient ftpClient = new FTPClient();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            log.debug("FtpClient: trying to connect");
            ftpClient.setConnectTimeout(connectionTimeout);
            ftpClient.connect(ftpHost, Integer.parseInt(ftpPort));

            boolean loggedIn = ftpClient.login(ftpUsername, ftpPassword);
            log.debug("FtpClient login success: " + loggedIn);

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            boolean retrieved = ftpClient.retrieveFile(fileUrl, baos);
            log.debug("File was retrieved: {}, Reply code is: {}, Reply string is: {}", retrieved, ftpClient.getReplyCode(), ftpClient.getReplyString());

            if (!retrieved) {
                log.error("FtpClient: Could not retrieve file " + fileUrl);
                throw new ClientException("Could not retrieve file " + fileUrl + ". Please try again!", ErrorCode.APPLICATION_ERROR);
            }

            return new ByteArrayResource(baos.toByteArray());
        } catch (IOException ex) {
            log.error("FtpClient: Could not retrieve file " + fileUrl, ex);
            throw new ClientException("Could not retrieve file " + fileUrl + ". Please try again!", ErrorCode.APPLICATION_ERROR);
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                log.error("Exception while trying to disconnect ftp client", ex);
            }
        }
    }

    /**
     * Utility to create an arbitrary directory hierarchy on the remote ftp server
     *
     * @param client  {@link FTPClient}
     * @param dirTree the directory tree only delimited with / chars. No file name!
     * @throws IOException if unable to create remote directory or change into it
     */
    private static void ftpCreateDirectoryTree(FTPClient client, String dirTree) throws IOException {
        boolean dirExists = true;

        log.debug("Trying to create FTP directory tree");
        // tokenize the string and attempt to change into each directory level.  If you cannot, then start creating.
        String[] directories = dirTree.split("/");
        log.debug("Current directory : {}",client.printWorkingDirectory());
        log.debug("Full directory tree: [%s]".formatted(dirTree));
        for (String dir : directories) {
            if (!dir.isEmpty()) {
                if (dirExists) {
                    dirExists = client.changeWorkingDirectory(dir);
                    log.debug("Dir exists: [%s], current working directory: [%s]".formatted(dirExists, dir));
                }
                if (!dirExists) {
                    log.error("Directory: [%s] does not exists, creating new one".formatted(dir));
                    if (!client.makeDirectory(dir)) {
                        log.error("Unable to create remote directory [%s], error=[(%s)(%s)].".formatted(dir, client.getReplyCode(), client.getReplyString()));
                        throw new IOException("Unable to create remote directory [%s], error=[(%s)(%s)].".formatted(dir, client.getReplyCode(), client.getReplyString()));
                    }
                    if (!client.changeWorkingDirectory(dir)) {
                        log.error("Unable to change into newly created remote directory '%s'.  error='%s'".formatted(dir, client.getReplyString()));
                        throw new IOException("Unable to change into newly created remote directory '%s'.  error='%s'".formatted(dir, client.getReplyString()));
                    }
                }
            }
        }
        log.debug("FTP Directory tree: [%s] created successfully".formatted(dirTree));
    }
}
