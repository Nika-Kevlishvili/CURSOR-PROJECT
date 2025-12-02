package bg.energo.phoenix.service.signing.signatus;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.request.document.SignatusSaveDocRequest;
import bg.energo.phoenix.model.response.document.SignatusDocumentResponse;
import bg.energo.phoenix.model.response.proxy.FileContent;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.service.archivation.edms.SignedDocumentFileArchivation;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.signing.SignerChainManager;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignatusSigningService {

    private final DocumentsRepository documentsRepository;
    private final SignedDocumentFileArchivation signedDocumentFileArchivation;
    private final SignerChainManager signerChainManager;
    private final Environment environment;
    private final FileService fileService;
    @Value("${signatus.threadCount}")
    private Integer threadCount;
    @Value("${signatus.auth.key}")
    private String SIGNATUS_KEY;
    @Value("${signatus.file.path}")
    private String SIGNATUS_FILE_PATH;

    @SneakyThrows
    @Transactional
    public List<SignatusDocumentResponse> getDocuments(String username, String key) {
        if (!SIGNATUS_KEY.equals(key)) {
            throw new ClientException("Unauthorized", ErrorCode.ACCESS_DENIED);
        }
        List<SignatusDocumentResponse> responseList = new ArrayList<>();
        List<Document> documentList = documentsRepository.findDocumentsForUser(username, LocalDateTime.now().minusDays(30));
        List<Callable<Boolean>> runnables = new ArrayList<>();
        if (!CollectionUtils.isEmpty(documentList)) {
            for (Document item : documentList) {

                runnables.add(() -> {
                    SignatusDocumentResponse response = SignatusDocumentResponse.builder()
                            .id(item.getId())
                            .fileName(item.getName().endsWith(".pdf") ? item.getName() : item.getName() + ".pdf")
                            .file(item.getName().endsWith(".pdf") ? item.getName().replace(".pdf", "") : item.getName())
                            .content(getFileInBase64(CollectionUtils.isEmpty(item.getSignedBy()) ? item.getUnsignedFileUrl() : item.getSignedFileUrl()))
                            .portal(getEnvironment(environment))
                            .kid(item.getSystemUserId())
                            .build();
                    responseList.add(response);
                    return true;
                });

            }
        }
        Executors.newFixedThreadPool(threadCount).invokeAll(runnables);
        return responseList;
    }

    private String getFileInBase64(String unsignedFileUrl) {
        ByteArrayResource file = fileService.downloadFile(unsignedFileUrl);
        if (file != null) {
            byte[] bytes = file.getByteArray();
            return Base64.getEncoder().encodeToString(bytes);
        }
        return null;
    }

    public String getEnvironment(Environment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        String[] defaultProfiles = environment.getDefaultProfiles();
        String environmentName = activeProfiles.length > 0 ? String.join(", ", activeProfiles) : String.join(", ", defaultProfiles);
        return "phoenix-" + environmentName;
    }

    @Transactional
    public void saveDocumentsForSignatus(SignatusSaveDocRequest request) {
        if (!SIGNATUS_KEY.equals(request.getKey())) {
            throw new ClientException("Unauthorized", ErrorCode.ACCESS_DENIED);
        }
        Optional<Document> documentOptional = documentsRepository.findById(request.getId());
        if (documentOptional.isPresent()) {
            Document document = documentOptional.get();
            List<DocumentSigners> signers = document.getSigners();
            if (!signers.contains(DocumentSigners.SIGNATUS)) {
                throw new IllegalArgumentException("File should not be signed by signatus;");
            }
            List<DocumentSigners> signedBy = document.getSignedBy();
            if (signedBy.contains(DocumentSigners.SIGNATUS)) {
                throw new IllegalArgumentException("File is already signed by signatus!;");
            }
            File file = getFileFromBase64(request.getFile());
            String randomName = UUID.randomUUID().toString();
            String fileName = randomName + ".pdf";
            String path = fileService.uploadFile(file, SIGNATUS_FILE_PATH, fileName);
            document.setSignedFileUrl(path);
//            document.setSignedFileName(fileName);

            signedBy.add(DocumentSigners.SIGNATUS);
            document.setSignedBy(signedBy);

            if (signedBy.size() == signers.size()) {
                document.setDocumentStatus(DocumentStatus.SIGNED);
                signedDocumentFileArchivation.archiveSignedFile(document);
                documentsRepository.saveAndFlush(document);
                return;
            }
            ArrayList<Document> documents = new ArrayList<>();
            documents.add(documentsRepository.save(document));
            signerChainManager.startSign(documents);
            documentsRepository.saveAndFlush(document);
        } else {
            signerChainManager.startSign(new ArrayList<>());
        }
    }

    private File getFileFromBase64(String base64String) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64String);
            File tempFile = Files.createTempFile(null, null).toFile();
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(decodedBytes);
            }
            return tempFile;
        } catch (Exception e) {
            throw new ClientException("Can't create file", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }

    /**
     * Downloads a document from the file service based on the provided document ID.
     *
     * @param id the ID of the document to download
     * @return a {@link FileContent} object containing the document name and byte array of the file
     * @throws DomainEntityNotFoundException if the document with the provided ID is not found
     */
    public FileContent downloadDocument(Long id) {
        Document document = documentsRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Document with presented id not found;"));

        ByteArrayResource resource = fileService.downloadFile(document.getSignedFileUrl());

        return new FileContent(document.getName(), resource.getByteArray());
    }

}



















