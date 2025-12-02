package bg.energo.phoenix.service.document.ftpService;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.util.ByteMultiPartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.MDC;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class FtpConnection {

    private final FTPClient ftpClient;

    public FtpConnection(int connectionTimeout, String ftpHost, int ftpPort, String username, String password) throws Exception {
        MDC.put("FtpLog", "CREATE");
        this.ftpClient = new FTPClient();
        ftpClient.setConnectTimeout(connectionTimeout);
        ftpClient.connect(ftpHost, ftpPort);
        boolean login = ftpClient.login(username, password);
        if (!login) {
            throw new ClientException("FtpClient login failure", ErrorCode.ACCESS_DENIED);
        }
        MDC.remove("FtpLog");
    }

    public String uploadFile(String remotePath, String name, InputStream inputStream) {
        long startTime = System.currentTimeMillis();
        log.debug("Upload start time {}", startTime);
        name = name.replaceAll("\\s+", "");
        try {
            log.debug("Resetting client!;");
            log.debug(ftpClient.printWorkingDirectory());
            resetClient();
            log.debug("Client after reset {}", ftpClient.printWorkingDirectory());
            ftpCreateDirectoryTree(ftpClient, remotePath);

            String fullPath = remotePath + "/" + name;
            boolean stored = ftpClient.storeFile(name, inputStream);
            if (!stored) {
                throw new ClientException("Could not store file " + fullPath + ". Please try again!", ErrorCode.APPLICATION_ERROR);
            }
            log.debug("Upload end time {}", System.currentTimeMillis() - startTime);

            return fullPath;
        } catch (IOException e) {
            throw new ClientException("FtpClient: Could not store file: %s, Reply code is: %s, Reply string is: %s".formatted(name, ftpClient.getReplyCode(), ftpClient.getReplyString()), ErrorCode.APPLICATION_ERROR);
        }
    }

    private void resetClient() throws IOException {
        long currentTimeMillis = System.currentTimeMillis();
        if (ftpClient.getDataConnectionMode() != FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE) {
            ftpClient.enterLocalPassiveMode();
        }
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.changeWorkingDirectory("/home/vsftpduser");
        log.debug("ResetTime: {}", currentTimeMillis);
    }

    public boolean downloadFile(String fileUrl, ByteArrayOutputStream outputStream) throws IOException {
        log.debug("Starting downloading");
        log.debug("Is good connection {}", ftpClient.isConnected());
        log.debug("Download directory {}", ftpClient.printWorkingDirectory());
        resetClient();
        return ftpClient.retrieveFile(fileUrl, outputStream);
    }

    public void disconnect() throws Exception {
        if (ftpClient.isConnected()) {
            ftpClient.logout();
            ftpClient.disconnect();
        }
    }

    public boolean isConnected() {
        boolean connected = ftpClient.isConnected();
        try {
            ftpClient.printWorkingDirectory();
        }catch (IOException e){
            connected=false;
        }
        return connected;
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


    /**
     * Deletes a file or directory on a remote FTP server given the remote path.
     *
     * @param remotePath the path to the file or directory on the remote FTP server
     * @throws ClientException if an error occurs while deleting the file or directory
     */
    public void deleteOnPath(String remotePath) {
        try {
            resetClient();
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
        }
    }

    public List<MultipartFile> fetchFilesFromFolder(String folderPath) throws Exception {
        log.debug("FtpClient starting to fetch files from folder: " + folderPath);
        List<MultipartFile> files = new ArrayList<>();
        resetClient();

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

    }

    private MultipartFile constructMultipartFile(FTPFile file, byte[] content) {
        if (file.getName().endsWith(".txt")) {
            return new ByteMultiPartFile(file.getName(), content, "text/plain");
        }
        return new ByteMultiPartFile(file.getName(), content);
    }
}
