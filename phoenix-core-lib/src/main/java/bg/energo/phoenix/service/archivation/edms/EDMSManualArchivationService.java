package bg.energo.phoenix.service.archivation.edms;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.model.request.communication.edms.CreateDocumentRequest;
import bg.energo.phoenix.model.request.communication.edms.PublishDocumentRequest;
import bg.energo.phoenix.model.request.communication.edms.UploadFileResponse;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAPIConfigurationProperties;
import bg.energo.phoenix.service.archivation.edms.config.EDMSProcessConfigurationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EDMSManualArchivationService {
    private final EDMSAPIConfigurationProperties apiProperties;
    private final EDMSProcessConfigurationProperties processProperties;
    private final EDMSRegistrationBundle edmsRegistrationBundle;

    public UploadFileResponse uploadFile(String description, Boolean asNewRevision, FileSystemResource fileSystemResource, List<Attribute> attributes) {
        Long documentId;
        Long fileId;
        try {
            edmsRegistrationBundle.login();

            documentId = createDocument();
            setAttributes(documentId, ListUtils.emptyIfNull(attributes));
            fileId = uploadFile(documentId, fileSystemResource, description);
            publishDocument(documentId, asNewRevision);
        } catch (WebClientResponseException e) {
            log.debug(e.getMessage());
            throw new ClientException("EDMS Manual Archivation Service: exception handled during upload file - [%s]".formatted(e.getResponseBodyAsString(StandardCharsets.UTF_8)), ErrorCode.APPLICATION_ERROR);
        } catch (WebClientRequestException e) {
            log.debug(e.getMessage());
            throw new ClientException("EDMS Manual Archivation Service: exception handled during upload file - [%s]".formatted(e.getMessage()), ErrorCode.APPLICATION_ERROR);
        } catch (Exception e) {
            log.debug(e.getMessage());
            throw new ClientException("EDMS Manual Archivation Service: exception handled during upload file", ErrorCode.APPLICATION_ERROR);
        } finally {
            edmsRegistrationBundle.logout();
        }
        return new UploadFileResponse(description, documentId, fileId);
    }

    private Long createDocument() {
        Mono<String> createDocumentResponse = edmsRegistrationBundle
                .getWebClient()
                .post()
                .uri(("%s%s".formatted(apiProperties.getUrl(), apiProperties.getCreateDocument())))
                .body(BodyInserters
                        .fromValue(
                                CreateDocumentRequest
                                        .builder()
                                        .documentTypeId(processProperties.getDocumentTypeId())
                                        .dateCreated(LocalDateTime.now())
                                        .folderId(processProperties.getFolderId())
                                        .build()
                        )
                )
                .headers(httpHeaders -> {
                    httpHeaders.add(HttpHeaders.AUTHORIZATION, edmsRegistrationBundle.getAccessToken());
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                })
                .accept(MediaType.TEXT_PLAIN)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(processProperties.getGlobalTimeout()));

        String block = createDocumentResponse.block();
        if (StringUtils.isEmpty(block)) {
            throw new ClientException("EDMS Manual Archivation Service: create document failed, document id is null", ErrorCode.APPLICATION_ERROR);
        }

        return Long.valueOf(block);
    }

    private void setAttributes(Long documentId, List<Attribute> attributes) {
        if (CollectionUtils.isEmpty(attributes)) {
            return;
        }

        edmsRegistrationBundle
                .getWebClient()
                .put()
                .uri(("%s%s".formatted(apiProperties.getUrl(), apiProperties.getSetAttributes().formatted(documentId))))
                .body(BodyInserters.fromValue(attributes))
                .headers(httpHeaders -> {
                    httpHeaders.add(HttpHeaders.AUTHORIZATION, edmsRegistrationBundle.getAccessToken());
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                })
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(Duration.ofSeconds(processProperties.getGlobalTimeout()))
                .block();
    }

    private Long uploadFile(Long documentId, FileSystemResource fileSystemResource, String description) {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("uploadedFile", fileSystemResource);

        return executeUploadFileCall(documentId, description, multipartBodyBuilder);
    }

    private Long executeUploadFileCall(Long documentId, String description, MultipartBodyBuilder multipartBodyBuilder) {
        Mono<String> uploadFileResponse = edmsRegistrationBundle
                .getWebClient()
                .post()
                .uri(("%s%s".formatted(apiProperties.getUrl(), apiProperties.getUploadFiles().formatted(documentId))), uriBuilder -> {
                    uriBuilder.queryParam("description", description);
                    uriBuilder.queryParam("allowAlteringContent", false);
                    return uriBuilder.build();
                })
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
                .headers(httpHeaders -> {
                    httpHeaders.add(HttpHeaders.AUTHORIZATION, edmsRegistrationBundle.getAccessToken());
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                })
                .accept(MediaType.TEXT_PLAIN)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(processProperties.getGlobalTimeout()));

        String block = uploadFileResponse.block();
        if (block == null || StringUtils.isEmpty(block)) {
            throw new ClientException("EDMS Manual Archivation Service: upload file failed, file id is null", ErrorCode.APPLICATION_ERROR);
        }

        return Long.valueOf(block);
    }

    private void publishDocument(Long documentId, Boolean asNewRevision) {
        edmsRegistrationBundle
                .getWebClient()
                .post()
                .uri(("%s%s".formatted(apiProperties.getUrl(), apiProperties.getPublishDocument().formatted(documentId))))
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters
                        .fromValue(
                                PublishDocumentRequest
                                        .builder()
                                        .asNewRevision(asNewRevision)
                                        .build())
                )
                .headers(httpHeaders -> {
                    httpHeaders.add(HttpHeaders.AUTHORIZATION, edmsRegistrationBundle.getAccessToken());
                    httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                })
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(Duration.ofSeconds(processProperties.getGlobalTimeout()))
                .block();
    }

    public ByteArrayResource downloadFile(Long documentId, Long fileId) {
        ByteArrayResource resource;
        try {
            edmsRegistrationBundle.login();

            Flux<ByteArrayResource> resourceResponse = edmsRegistrationBundle
                    .getWebClient()
                    .get()
                    .uri(("%s%s".formatted(apiProperties.getUrl(), apiProperties.getDownloadFiles().formatted(documentId, fileId))))
                    .headers(httpHeaders -> httpHeaders.add(HttpHeaders.AUTHORIZATION, edmsRegistrationBundle.getAccessToken()))
                    .retrieve()
                    .bodyToFlux(ByteArrayResource.class)
                    .timeout(Duration.ofSeconds(processProperties.getGlobalTimeout()));

            resource = resourceResponse.blockLast();
        } catch (WebClientResponseException e) {
            log.debug(e.getMessage());
            throw new ClientException("EDMS Manual Archivation Service: exception handled during upload file - [%s]".formatted(e.getResponseBodyAsString(StandardCharsets.UTF_8)), ErrorCode.APPLICATION_ERROR);
        } catch (Exception e) {
            throw new ClientException("EDMS Manual Archivation Service: exception handled on download file", ErrorCode.APPLICATION_ERROR);
        } finally {
            edmsRegistrationBundle.logout();
        }
        return resource;
    }

    public void deleteDocument(Long documentId) {
        try {
            edmsRegistrationBundle.login();

            edmsRegistrationBundle
                    .getWebClient()
                    .delete()
                    .uri(("%s%s".formatted(apiProperties.getUrl(), apiProperties.getDeleteDocument()).formatted(documentId)))
                    .headers(httpHeaders -> httpHeaders.add(HttpHeaders.AUTHORIZATION, edmsRegistrationBundle.getAccessToken()))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(processProperties.getGlobalTimeout()))
                    .block();
        } catch (WebClientResponseException e) {
            log.debug(e.getMessage());
            throw new ClientException("EDMS Manual Archivation Service: exception handled while trying to delete document - [%s]".formatted(e.getResponseBodyAsString(StandardCharsets.UTF_8)), ErrorCode.APPLICATION_ERROR);
        } catch (Exception e) {
            throw new ClientException("EDMS manual Archivation Service: exception handled while trying to delete document", ErrorCode.APPLICATION_ERROR);
        } finally {
            edmsRegistrationBundle.logout();
        }
    }
}
