package bg.energo.phoenix.process.controller;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.util.UrlEncodingUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/ftp-test")
public class FtpTestController {

    @Value("${ftp.server.host}")
    String ftpHost;

    @Value("${ftp.server.port}")
    String ftpPort;

    @Value("${ftp.server.username}")
    String ftpUsername;

    @Value("${ftp.server.password}")
    String ftpPassword;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @Operation(
            security = @SecurityRequirement(name = "bearer-token")
    )
    public ResponseEntity<Void> upload(@RequestParam("file") MultipartFile file, String remotePath) {
        boolean stored = uploadFile(file, remotePath);
        if (stored) {
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(value = "/download")
    @Operation(
            security = @SecurityRequirement(name = "bearer-token")
    )
    public HttpEntity<ByteArrayResource> download(String remotePath) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "force-download"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=%s".formatted(UrlEncodingUtil.encodeFileName(remotePath.substring(remotePath.lastIndexOf("/")))));
        return new HttpEntity<>(downloadFile(remotePath), headers);
    }

    @GetMapping(value = "/list-folder")
    @Operation(
            security = @SecurityRequirement(name = "bearer-token")
    )
    public ResponseEntity<List<String>> list(String remotePath) {
        return ResponseEntity.ok(listFolder(remotePath));
    }

    @DeleteMapping(value = "/delete")
    @Operation(
            security = @SecurityRequirement(name = "bearer-token")
    )
    public ResponseEntity<Void> delete(String remotePath) {
        deleteOnPath(remotePath);
        return ResponseEntity.ok().build();
    }

    private void deleteOnPath(String remotePath) {
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

    private List<String> listFolder(String remotePath) {
        FTPClient ftpClient = new FTPClient();
        try {
            log.debug("Before ftp connect");
            ftpClient.connect(ftpHost, Integer.parseInt(ftpPort));
            log.debug("before ftp login");

            ftpClient.login(ftpUsername, ftpPassword);
            ftpClient.enterLocalPassiveMode();

            FTPFile[] ftpFiles = ftpClient.listFiles(remotePath);

            return Arrays.stream(ftpFiles).map(FTPFile::getName).toList();
        } catch (Exception e) {
            log.error("Exception handled while listing folders on path: %s".formatted(remotePath), e);
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

    public boolean uploadFile(MultipartFile file, String remotePath) {
        FTPClient ftpClient = new FTPClient();

        try (InputStream inputStream = file.getInputStream()) {
            log.debug("Before ftp connect");
            ftpClient.connect(ftpHost, Integer.parseInt(ftpPort));
            log.debug("before ftp login");
            ftpClient.login(ftpUsername, ftpPassword);

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            boolean stored = ftpClient.storeFile(remotePath, inputStream);
            log.debug("File was stored: " + stored);
            log.debug("Reply code is: " + ftpClient.getReplyCode());
            log.debug("Reply string is: " + ftpClient.getReplyString());

            if (!stored) {
                log.error("FtpClient: Could not store file");
                throw new ClientException("Could not store file " + remotePath + ". Please try again!", ErrorCode.APPLICATION_ERROR);
            }

            return true;
        } catch (IOException ex) {
            log.error("FtpClient: Could not store file " + remotePath, ex);
            throw new ClientException("Could not store file " + remotePath + ". Please try again!", ErrorCode.APPLICATION_ERROR);
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

    public ByteArrayResource downloadFile(String remotePath) {
        FTPClient ftpClient = new FTPClient();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            log.debug("Before ftp connect");
            ftpClient.connect(ftpHost, Integer.parseInt(ftpPort));
            log.debug("before ftp login");
            ftpClient.login(ftpUsername, ftpPassword);

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            boolean retrieved = ftpClient.retrieveFile(remotePath, baos);
            log.debug("File was retrieved: " + retrieved);
            log.debug("Reply code is: " + ftpClient.getReplyCode());
            log.debug("Reply string is: " + ftpClient.getReplyString());

            if (!retrieved) {
                log.error("FtpClient: Could not retrieve file " + remotePath);
                throw new ClientException("Could not retrieve file " + remotePath + ". Please try again!", ErrorCode.APPLICATION_ERROR);
            }

            return new ByteArrayResource(baos.toByteArray());
        } catch (IOException ex) {
            log.error("FtpClient: Could not download file " + remotePath, ex);
            throw new ClientException("Could not download file " + remotePath + ". Please try again!", ErrorCode.APPLICATION_ERROR);
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
}
