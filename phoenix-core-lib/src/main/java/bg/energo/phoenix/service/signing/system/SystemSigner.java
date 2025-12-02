package bg.energo.phoenix.service.signing.system;

import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.service.archivation.edms.FileArchivationService;
import bg.energo.phoenix.service.archivation.edms.SignedDocumentFileArchivation;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.signing.SignerChain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemSigner implements SignerChain {

    private final FileService fileService;

    private final DocumentsRepository documentsRepository;
    private final FileArchivationService fileArchivationService;
    private final SignedDocumentFileArchivation signedDocumentFileArchivation;

    @Value("${system.certificate.jSign}")
    private String jSignPath;

    @Value("${system.certificate.alg}")
    private String kst;
    @Value("${system.certificate.cert-secret}")
    private String ksp;//PASSWORD
    @Value("${system.certificate.key-alias}")
    private String ka;//CertName
    @Value("${system.certificate.key-secret}")
    private String kp;//Cert password
    @Value("${system.certificate.keystore-path}")
    private String certPath;


    @Value("${system.certificate.signed-base-path}")
    private String signBasePath;
    @Value("${system.certificate.unsigned-base-path}")
    private String unsignedBasePath;

    @Value("${system.certificate.signed-remote-path}")
    private String signeFileRemoteLocation;

    @Override
    public DocumentSigners getSigner() {
        return DocumentSigners.SYSTEM_CERTIFICATE;
    }

    @Override
    public List<Document> sign(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            log.warn("No documents provided for signing.");
            return new ArrayList<>();
        }

        File unsignedBaseFolder = new File(unsignedBasePath);
        File unsignedFolder = new File(unsignedBaseFolder, UUID.randomUUID().toString());

        try {
            boolean mkdir = unsignedFolder.mkdir();
            if (mkdir) {
                log.debug("Unsigned folder created successfully at {}", unsignedFolder.getAbsolutePath());
            } else {
                log.warn("Folder could not be created using mkdir(), trying Files.createDirectories()");
                Files.createDirectories(unsignedFolder.toPath());
                log.debug("Unsigned folder created using Files.createDirectories() at {}", unsignedFolder.getAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Failed to create unsigned folder: {}", e.getMessage(), e);
            throw new RuntimeException("Unable to create directory for unsigned documents", e);
        }

        for (Document document : documents) {
            try {
                ByteArrayResource byteArrayResource = fileService.downloadFile(document.getUnsignedFileUrl());
                File outputFile = new File(unsignedFolder, document.getId().toString() + ".pdf");
                Files.write(
                        outputFile.toPath(),
                        byteArrayResource.getByteArray(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                );
                log.debug("Written unsigned file for document ID {} to {}", document.getId(), outputFile.getAbsolutePath());
            } catch (IOException e) {
                log.error("Error writing unsigned file for document ID {}: {}", document.getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to write unsigned document file for document ID " + document.getId(), e);
            }
        }

        File[] files = unsignedFolder.listFiles();
        log.debug("Documents written to folder. Total files: {}", files == null ? 0 : files.length);

        List<Pair<String, String>> signedDocs;
        try {
            signedDocs = signDocumentsInFolder(unsignedFolder.getAbsolutePath() + "/*");
            log.debug("Documents signed successfully: {}", signedDocs);
        } catch (Exception e) {
            log.error("Failed to sign documents in folder {}: {}", unsignedFolder.getAbsolutePath(), e.getMessage(), e);
            throw new RuntimeException("Signing process failed", e);
        }

        Map<Long, Document> documentMap = documents.stream().collect(Collectors.toMap(Document::getId, doc -> doc));
        List<Document> partiallySignedDocs = new ArrayList<>();

        for (Pair<String, String> signedDoc : signedDocs) {
            try {
                Long docId = Long.valueOf(signedDoc.getFirst());
                Document document = documentMap.get(docId);
                if (document == null) {
                    log.warn("Signed document with ID {} not found in original document list", docId);
                    continue;
                }

                File signedFile = new File(signedDoc.getSecond());
                String uploadFileUrl = fileService.uploadFile(signedFile, signeFileRemoteLocation, UUID.randomUUID() + ".pdf");

                document.setSignedFileUrl(uploadFileUrl);
                document.getSignedBy().add(DocumentSigners.SYSTEM_CERTIFICATE);

                if (document.getSigners().size() == document.getSignedBy().size()) {
                    document.setDocumentStatus(DocumentStatus.SIGNED);
                    signedDocumentFileArchivation.archiveSignedFile(document);
                    log.debug("Document ID {} fully signed and archived", docId);
                } else {
                    log.debug("Document ID {} partially signed", docId);
                }

                Document savedDoc = documentsRepository.saveAndFlush(document);
                if (!DocumentStatus.SIGNED.equals(savedDoc.getDocumentStatus())) {
                    partiallySignedDocs.add(savedDoc);
                }
            } catch (Exception e) {
                log.error("Error processing signed document pair {}: {}", signedDoc, e.getMessage(), e);
            }
        }

        return partiallySignedDocs;
    }

    public List<Pair<String, String>> signDocumentsInFolder(String folder) throws IOException {
        log.debug("Starting to sign documents in folder: {}", folder);

        String signLocation = signBasePath + "/" + LocalDate.now() + "/" + UUID.randomUUID() + "/";
        File signFolder = new File(signLocation);

        if (!signFolder.exists()) {
            boolean created = signFolder.mkdirs();
            if (created) {
                log.debug("Created signing output directory at {}", signLocation);
            } else {
                log.warn("Failed to create signing output directory at {}", signLocation);
            }
        } else {
            log.debug("Signing output directory already exists: {}", signLocation);
        }

        String command = String.format(
                """
                        java -jar %s -kst %s -ksf %s -ksp %s -d %s -r GRAPHIC_AND_DESCRIPTION -ka %s -kp %s %s
                        """,
                jSignPath,
                kst,
                certPath,
                ksp,
                signLocation,
                ka,
                kp,
                folder
        );

        log.debug("Executing signing command: {}", command);

        try {
            getOutputFromProgram(command);
            log.debug("Finished executing signing command.");
        } catch (Exception e) {
            log.error("Error during document signing execution: {}", e.getMessage(), e);
            throw new IOException("Document signing failed", e);
        }

        File[] signedFiles = signFolder.listFiles();

        if (signedFiles == null || signedFiles.length == 0) {
            log.warn("No signed files found in output folder: {}", signFolder.getAbsolutePath());
            return List.of();
        }

        log.debug("Signed files found: {}", signedFiles.length);

        List<Pair<String, String>> signedDocList = Arrays
                .stream(signedFiles)
                .map(file -> Pair.of(parseName(file.getName()), file.getAbsolutePath()))
                .toList();

        log.debug("Processed {} signed files from {}", signedDocList.size(), signFolder.getAbsolutePath());

        return signedDocList;
    }


    private String parseName(String name) {
        return name.replace("_signed.pdf", "");
    }

//    @SneakyThrows
//    public void signDocuments(Long documentFileIds) {
//        DocumentFiles documentsToSign = documentFileRepository.findByDocumentId(documentFileIds).get();
//
//        log.debug("Cert {}",Arrays.stream(Objects.requireNonNull(new File("/usr/share/certs/").listFiles())).toList());
//        File folder = new File(unsignedBasePath + LocalDate.now() + "/" + UUID.randomUUID() + "/");
//        if(folder.mkdirs()){
//            log.debug("Created folder {}", folder.getAbsolutePath());
//        }
//        ByteArrayResource byteArrayResource = fileService.downloadFile(documentsToSign.getUnsignedFileUrl());
//        File unsignedFile = new File(folder.getAbsolutePath() +"/bla.pdf");
//        if(unsignedFile.createNewFile()){
//            log.debug("Created file {}",unsignedFile.getAbsolutePath());
//        }
//        FileUtils.writeByteArrayToFile(unsignedFile, byteArrayResource.getByteArray());
//        String absolutePath = folder.getAbsolutePath();
//        List<Pair<String, String>> pairs = signDocumentsInFolder(absolutePath.endsWith("/") ? absolutePath + "*" : absolutePath + "/*");
//
//        if(!CollectionUtils.isEmpty(pairs)) {
//            File file = new File(pairs.get(0).getSecond());
//
//            log.debug("Uploading file {}",file.getName());
//            String s = fileService.uploadFile(file, signeFileRemoteLocation, file.getName());
//            documentsToSign.setSignedFileUrl(s);
//            documentFileRepository.save(documentsToSign);
//            log.debug("Uploaded file!");
//        }
//
//    }

//    private void uploadAndSave(List<Pair<String,String>> signedFileLocations, List<DocumentFiles> documentFiles) {
//        Map<String, DocumentFiles> filesMap = documentFiles.stream().collect(Collectors.toMap(DocumentFiles::getUnsignedFileName, j -> j));
//        for (int i = 0; i < signedFileLocations.size(); i++) {
//            Pair<String, String> signedPair = signedFileLocations.get(i);
//            String signedFileName = signedPair.getFirst();
//            DocumentFiles files = filesMap.get(signedFileName);
//            files.setSignedFileName(signedFileName);
//            files.setSignedFileUrl(fileService.uploadFile(new File(signedPair.getSecond()),signeFileRemoteLocation,signedFileName));
//        }
//    }

    private String getOutputFromProgram(String program) throws IOException {
        Process proc = Runtime.getRuntime().exec(program);
        return Stream
                .of(proc.getErrorStream(), proc.getInputStream())
                .map((InputStream isForOutput) -> {
                         try (BufferedReader br = new BufferedReader(new InputStreamReader(isForOutput))) {
                             while (br.readLine() != null) {
                                 log.debug("{}", br.readLine());
                             }
                         } catch (IOException e) {
                             throw new RuntimeException(e);
                         }
                         return "";
                     }
                )
                .collect(Collectors.joining());
    }
}
